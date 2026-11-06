# Hand-off: Cancelling an in-flight transaction remotely (`TransactionCancel`)

## Goal

Make the Python test app correctly cancel a transaction that is currently in
progress on the dsiEMVAndroid demo app (e.g. cancel a sale while the device is
waiting for the cardholder to dip/tap).

## TL;DR — the thing that bites people

`TransactionCancel` is **asynchronous and must be sent on a second, concurrent
connection** while the original transaction request is still open and blocking.

- The cancel `POST` itself returns an **empty body immediately**. It is *not* a
  request/response like a sale — it's a fire-and-forget signal.
- The **result of the cancel comes back on the ORIGINAL transaction's response**
  (the still-open sale `POST`), which unblocks and returns
  `"Transaction Not Complete"` (or similar).
- Therefore, if the Python app sends requests **serially on one connection**, it
  can never cancel — it's stuck waiting on the sale `POST` and can't send the
  cancel until that `POST` already returned (too late). **You need two
  concurrent requests** (two threads, async, or two connections).

If your bug is "the cancel does nothing" or "the cancel hangs / times out," this
concurrency requirement is almost always the cause.

---

## How it works on the Android side (the contract)

The app's HTTP listener (`LocalListenerService.java` → `LocalListener.serve()`)
dispatches a `POST` like this:

```java
if      (path contains "collectcarddata")      -> CollectCardData(xml)   // returns RStream
else if (body contains "TransactionCancel")    -> CancelRequest()        // returns "" (empty)
else                                           -> ProcessTransaction(xml) // returns RStream
```

Key facts:

| Aspect | Behavior |
|--------|----------|
| How it's detected | The request **body** contains the exact substring `TransactionCancel` |
| Case sensitivity | **Case-sensitive** — `transactioncancel` will NOT match; send exactly `TransactionCancel` |
| Path | Body-based, so path is mostly ignored — **BUT** do not post the cancel to `/collectcarddata` (that path routes to CollectCardData first). Use `/`. |
| Response to the cancel POST | **Empty body**, HTTP 200. Do not wait on it for the cancel result. |
| Where the result appears | On the **original** `ProcessTransaction` response, which unblocks and returns e.g. `Transaction Not Complete` |
| Underlying call | `CancelRequest()` (void) → native `CancelAndroidTransaction()` |
| Timing limit | Only works **before a successful card read**. After the card is read, the cancel is **ignored** (Datacap behavior). |

---

## Required client flow

```
Thread/connection A:  POST /            <sale XML>            -> blocks while cardholder interacts
Thread/connection B:  POST /            <TransactionCancel>   -> returns "" immediately
                                                                  (CancelRequest fires on device)
Thread/connection A:  ... unblocks  -> returns "Transaction Not Complete"   <-- the real result
```

1. Start the sale on connection A (it will block).
2. While A is still open, send the cancel on connection B.
3. Read the cancel outcome from **A's** response, not B's.

> Do **not** close/abort connection A to "cancel" — that just drops your read of
> the result; the device may still be running the transaction. Always send the
> explicit `TransactionCancel` and then read A's response.

---

## Request body

The listener only checks for the substring `TransactionCancel`, so the exact
shape is flexible, but send a well-formed request consistent with your other
calls. Example:

```xml
<?xml version="1.0"?>
<TStream>
    <Admin>
        <TranCode>TransactionCancel</TranCode>
    </Admin>
</TStream>
```

(If your existing app already sends a specific cancel payload that the device
accepted before, keep using it — just ensure the literal string
`TransactionCancel` is present, with that exact casing.)

Encoding: send the body as **ISO-8859-1 / Latin-1**, same as all other requests
to this listener.

---

## Python reference implementation

```python
import threading
import requests

LISTENER_HOST = "192.168.0.50"
LISTENER_PORT = 8080
BASE_URL = f"http://{LISTENER_HOST}:{LISTENER_PORT}/"

CANCEL_XML = """<?xml version="1.0"?>
<TStream><Admin><TranCode>TransactionCancel</TranCode></Admin></TStream>"""


def _post(body: str, timeout: float) -> str:
    resp = requests.post(BASE_URL, data=body.encode("latin-1"), timeout=timeout)
    resp.raise_for_status()
    return resp.text


def run_sale_with_cancel(sale_xml: str, cancel_after_seconds: float = 5.0) -> str:
    """Start a sale, then cancel it after a delay. Returns the SALE response
    (which carries the cancellation result)."""
    result = {}

    def do_sale():
        # Long timeout: this blocks until the device finishes or is cancelled.
        result["sale"] = _post(sale_xml, timeout=120)

    sale_thread = threading.Thread(target=do_sale)
    sale_thread.start()

    # Give the sale time to reach the device and start prompting, THEN cancel
    # on this (separate) connection while the sale POST is still open.
    timer = threading.Timer(cancel_after_seconds, lambda: _post(CANCEL_XML, timeout=30))
    timer.start()

    sale_thread.join()
    timer.cancel()  # no-op if already fired

    return result.get("sale", "")   # contains "Transaction Not Complete" if cancelled in time


if __name__ == "__main__":
    sale = """<?xml version="1.0"?>
<TStream><Transaction>
    <MerchantID>YOUR_MID</MerchantID>
    <POSPackageID>DSIEMVAndroidDemo:1.00</POSPackageID>
    <TranCode>EMVSale</TranCode>
    <SecureDevice>EMV_LANE3000_DATACAP_E2E</SecureDevice>
    <OperationMode>CERT</OperationMode>
    <Amount><Purchase>1.00</Purchase></Amount>
    <SequenceNo>0010010010</SequenceNo>
    <PinPadIpAddress>192.168.0.59</PinPadIpAddress>
    <PinPadIpPort>12000</PinPadIpPort>
</Transaction></TStream>"""
    print(run_sale_with_cancel(sale, cancel_after_seconds=5))
```

### Common pitfalls (likely sources of the bug)

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Cancel "does nothing" | Sent serially — the sale `POST` hadn't been sent yet, or had already returned | Send cancel on a **separate concurrent connection** while the sale `POST` is open |
| Cancel `POST` hangs / times out waiting for a body | Code waits for/parses the cancel response | The cancel returns **empty**; read the result from the **sale** response instead |
| Cancel ignored | Card was already read before the cancel arrived | Cancel earlier; it only works **before** a successful card read |
| Cancel never triggers | Wrong casing (`transactioncancel`) or string missing from body | Body must contain the exact substring **`TransactionCancel`** |
| Cancel routed to CollectCardData | Cancel posted to `/collectcarddata` | Post the cancel to **`/`** |
| Single-connection HTTP client serializes everything | Using one `requests.Session` with default pooling that blocks | Use a separate thread/connection, or `requests` default (new connection) as shown above |

---

## Optional Android-side improvement (not required)

Today the cancel branch returns an empty body. If it would help the Python app to
get an explicit acknowledgement that the cancel signal was received (separate from
the actual cancellation result on the sale connection), the listener could be
changed to return a small ack, e.g.:

```java
else if (UTF8RequestMsg.contains("TransactionCancel")) {
    dsiEMVAndroidinstance.getInstance(context).CancelRequest();
    returnMSG = "<RStream><CmdStatus>CancelRequested</CmdStatus></RStream>";
}
```

This is **optional** and does not change the core model (the real outcome still
comes back on the original transaction's response). Flag if you want this added.

---

## Test checklist

1. Start a sale to a real device so it prompts for a card; **do not** present a card.
2. While the sale `POST` is still open, send `TransactionCancel` on a second connection.
3. Confirm the **sale** response returns `Transaction Not Complete` (or similar).
4. Confirm presenting a card *before* sending cancel results in the cancel being ignored (normal sale completes).
5. Confirm the cancel `POST` itself returns quickly with an empty body.

---

## Android-side references

- Routing / cancel branch: `LocalListenerService.java` → inner `LocalListener.serve()`.
- On-device equivalent: `MainActivity.java` cancel button → `CancelRequest()` (line ~361).
- AAR method: `void CancelRequest()` (native `CancelAndroidTransaction()`).
