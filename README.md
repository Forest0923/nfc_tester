# NFC_tester

## About

This application is designed for reading NFC tag information and memory data.
Currently, only FeliCa (NfcF) is supported.

## References

This application is strongly inspired by following applications.

- [NXP NFC Project](https://github.com/NXPNFCProject)

The following documents, tech notes, and applications were very helpful in the development process.

- [Android developers guide - NFC](https://developer.android.google.cn/guide/topics/connectivity/nfc/nfc)
- [Android developers guide - NfcF](https://developer.android.com/reference/kotlin/android/nfc/tech/NfcF)
- [SONY - FeliCa Technical Information](https://www.sony.net/Products/felica/business/tech-support/)
- [Metrodroid](https://github.com/metrodroid)
- [[PASMO] FeliCa から情報を吸い出してみる - FeliCaの仕様編 [Android][Kotlin]](https://qiita.com/YasuakiNakazawa/items/3109df682af2a7032f8d)
- [AndroidでFelica(NFC)のブロックデータの取得](https://qiita.com/nshiba/items/38f94d61c020a17314b6)

## Known Issues

- I intended to read NFC only with the main activity, but `PendingIntent` throws an error on Android 12, so it's not currently realized.
- The application crashes when reading NFC except NfcF.