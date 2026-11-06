# Hand-off: Calling `CollectCardData` remotely from the Python test app

## Goal

Add support to the Python desktop test app for triggering a **CollectCardData**
(pre-dip / pre-tap card data collection) on the dsiEMVAndroid demo app, over the
same local HTTP listener already used for sales.

No new transport, port, or library is needed. This is the **same** `POST` to the
Android device's listener you already use for `ProcessTransaction` sales — the
only differences are the **URL path**, the **TranCode**, and using
**`PlaceHolderAmount`** instead of `Amount`.

---

## The contract

| Item | Value |
|------|-------|
| Method | `POST` |
| Host | `http://<android-device-ip>:8080` |
| Path (sales — unchanged) | `/` |
| **Path (CollectCardData — NEW)** | **`/collectcarddata`** (case-insensitive; matched as a substring of the path) |
| Body | XML `TStream` request (see below) |
| Body encoding | **ISO-8859-1 / Latin-1** (the listener reads the raw body as Latin-1) |
| Response | XML `RStream` string, returned synchronously on the same connection |
| Timeout | Use a generous client timeout (≥ 60s) — the call blocks while the cardholder dips/taps |

**Routing is by path.** The Android listener dispatches a POST whose path
contains `collectcarddata` to the dedicated `CollectCardData()` control method;
all other POSTs continue to go to `ProcessTransaction()`. Do **not** rely on the
body to select the operation — always post CollectCardData to `/collectcarddata`.

---

## Important: `CollectCardData` is NOT a ProcessTransaction TranCode

This trips people up. The canonical dsiEMVAndroid TranCode list (`EMVSale`,
`EMVPreAuth`, `EMVReturn`, …) does **not** include `CollectCardData`. If you sent
`<TranCode>CollectCardData</TranCode>` to the normal sale path (`/`), it would be
rejected.

`CollectCardData` is a **separate control method** with its **own request
format**. That request format *does* use `<TranCode>CollectCardData</TranCode>`,
and the library passes the XML straight through to the device. So:

- Post to **`/collectcarddata`** → handled by the `CollectCardData()` method →
  `<TranCode>CollectCardData</TranCode>` is correct and expected.
- Post to **`/`** → handled by `ProcessTransaction()` → use a real TranCode
  (`EMVSale`, etc.).

---

## Request body

### Field reference

| Field | Required | Notes |
|-------|----------|-------|
| `MerchantID` | yes | Merchant ID assigned by the processor |
| `POSPackageID` | yes | `name:version`, e.g. `DSIEMVAndroidDemo:1.00` |
| `TranCode` | yes | Always `CollectCardData` for this call |
| `SecureDevice` | yes | Device-specific (see below) |
| `OperationMode` | yes | `CERT` for certification/testing, `PROD` for production |
| `PlaceHolderAmount` | yes | Temp amount (e.g. `1.00`) used to drive CVM behavior — **not** `Amount` |
| `SequenceNo` | yes | Use the `SequenceNo` from the last response; `0010010010` to (re)sync |
| `PinPadIpAddress` | IP pinpads only | LAN IP of the pinpad (e.g. Lane 3000) |
| `PinPadIpPort` | IP pinpads only | See per-device notes |

### `SecureDevice` / port per test device

| Device | `SecureDevice` | `PinPadIpPort` | `PinPadIpAddress` |
|--------|----------------|----------------|-------------------|
| Ingenico **Lane 3000** (IP) | `EMV_LANE3000_DATACAP_E2E` | the pinpad's port (e.g. `12000`) | the Lane 3000's LAN IP |
| **PAX** Android (on-pad) | model-specific, e.g. `EMV_A920PRO_DATACAP_E2E` (A920/A920Pro/A920MAX), `EMV_A35_DATACAP_E2E` (A35), `EMV_A77_DATACAP_E2E` (A77) | `1235` | the PAX device IP |

> The simplest rule: **take the exact request your Python app already sends for a
> working `EMVSale` on that device, then (1) change the path to
> `/collectcarddata`, (2) set `TranCode` to `CollectCardData`, and (3) replace the
> `Amount` element with `PlaceHolderAmount`.** Everything else (SecureDevice,
> PinPad IP/port, MerchantID) is identical to your sale.

### Example — Lane 3000

```xml
<?xml version="1.0"?>
<TStream>
    <Transaction>
        <MerchantID>YOUR_MID</MerchantID>
        <POSPackageID>DSIEMVAndroidDemo:1.00</POSPackageID>
        <TranCode>CollectCardData</TranCode>
        <SecureDevice>EMV_LANE3000_DATACAP_E2E</SecureDevice>
        <OperationMode>CERT</OperationMode>
        <PlaceHolderAmount>1.00</PlaceHolderAmount>
        <SequenceNo>0010010010</SequenceNo>
        <PinPadIpAddress>192.168.0.59</PinPadIpAddress>
        <PinPadIpPort>12000</PinPadIpPort>
    </Transaction>
</TStream>
```

### Example — PAX (A920 family)

```xml
<?xml version="1.0"?>
<TStream>
    <Transaction>
        <MerchantID>YOUR_MID</MerchantID>
        <POSPackageID>DSIEMVAndroidDemo:1.00</POSPackageID>
        <TranCode>CollectCardData</TranCode>
        <SecureDevice>EMV_A920PRO_DATACAP_E2E</SecureDevice>
        <OperationMode>CERT</OperationMode>
        <PlaceHolderAmount>1.00</PlaceHolderAmount>
        <SequenceNo>0010010010</SequenceNo>
        <PinPadIpAddress>192.168.0.60</PinPadIpAddress>
        <PinPadIpPort>1235</PinPadIpPort>
    </Transaction>
</TStream>
```

---

## Response body

The response is an `RStream` returned synchronously (same HTTP connection). Example:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<RStream>
    <ResponseOrigin>Client</ResponseOrigin>
    <DSIXReturnCode>000000</DSIXReturnCode>
    <CmdStatus>Success</CmdStatus>
    <TextResponse>Success</TextResponse>
    <SequenceNo>0010010010</SequenceNo>
    <CustomerData>NDc2MTczMDA0MzMxMTI=</CustomerData>
    <CardholderName>Datacap/Test Card 01      </CardholderName>
    <ExpDateMonth>12</ExpDateMonth>
    <ExpDateYear>31</ExpDateYear>
    <CardBin>476173</CardBin>
    <Last4>0043</Last4>
    <AccountSource>ChipRead</AccountSource>
    <AID>A0000000031010</AID>
    <CardType>VISA</CardType>
    <TranType>EMVCredit</TranType>
    <UserTrace />
</RStream>
```

Key fields to read:

| Field | Meaning |
|-------|---------|
| `CmdStatus` | `Success` on success; `Error` on failure (check `DSIXReturnCode` / `TextResponse`) |
| `DSIXReturnCode` | `000000` = success; non-zero identifies the error |
| `SequenceNo` | Persist and send as `SequenceNo` on the next request |
| `CardBin`, `Last4`, `CardType` | Card identification |
| `CustomerData` | Base-64 unique card identifier (e.g. parking in/out) |
| `AccountSource` | `Swipe`, `ChipRead`, `ContactlessChip`, etc. |

---

## Python reference implementation

```python
import requests

LISTENER_HOST = "192.168.0.50"   # the Android device running the demo app
LISTENER_PORT = 8080


def collect_card_data(xml_request: str) -> str:
    """POST a CollectCardData request and return the RStream response text."""
    url = f"http://{LISTENER_HOST}:{LISTENER_PORT}/collectcarddata"
    resp = requests.post(
        url,
        data=xml_request.encode("latin-1"),  # listener reads body as ISO-8859-1
        headers={"Content-Type": "application/xml"},
        timeout=120,                          # cardholder dip/tap can take a while
    )
    resp.raise_for_status()
    return resp.text


if __name__ == "__main__":
    request_xml = """<?xml version="1.0"?>
<TStream>
    <Transaction>
        <MerchantID>YOUR_MID</MerchantID>
        <POSPackageID>DSIEMVAndroidDemo:1.00</POSPackageID>
        <TranCode>CollectCardData</TranCode>
        <SecureDevice>EMV_LANE3000_DATACAP_E2E</SecureDevice>
        <OperationMode>CERT</OperationMode>
        <PlaceHolderAmount>1.00</PlaceHolderAmount>
        <SequenceNo>0010010010</SequenceNo>
        <PinPadIpAddress>192.168.0.59</PinPadIpAddress>
        <PinPadIpPort>12000</PinPadIpPort>
    </Transaction>
</TStream>"""

    print(collect_card_data(request_xml))
```

If your existing test app already has a "send transaction" helper that POSTs to
`/`, the cleanest integration is to add an optional `path` argument (defaulting to
`/`) and pass `/collectcarddata` for this call — reusing all of your existing
connection/encoding logic.

---

## Test checklist

1. App is running on the target device and the listener is up (port `8080`).
2. POST the Lane 3000 / PAX example to `http://<ip>:8080/collectcarddata`.
3. Dip/tap a test card when prompted on the pinpad.
4. Confirm the response has `CmdStatus=Success` and populated `CardBin` / `Last4`
   / `CardType`.
5. (Sanity) POST a normal `EMVSale` to `/` and confirm sales still work unchanged.

---

## Android-side reference (already implemented)

- Routing: `LocalListenerService.java` → inner `LocalListener.serve()` dispatches
  `POST /collectcarddata` to `dsiEMVAndroidinstance.CollectCardData(xml)`.
- Request builder (on-device equivalent): `MainActivity.setupCollectCardData(...)`.
- Response also surfaces in the app UI via `AddCollectCardDataResponseListener`.
