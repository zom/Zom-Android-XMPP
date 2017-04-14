Zom for Android (previously known as ChatSecure and Gibberbot) is a secure messaging app built on open standards like XMPP/Jabber and OTR encryption.

Learn more at https://zom.im

It includes OTR4J:
https://github.com/otr4j/otr4j

and BouncyCastle for Java:
http://www.bouncycastle.org/java.html

and SQLCipher for Android:
https://guardianproject.info/code/sqlcipher/

Original wallpaper generated using Tapet app and Gimp:
https://play.google.com/store/apps/details?id=com.sharpregion.tapet

and previously included some CC0 public domain beautiful images:
Ry Van
https://unsplash.com/ryvanveluwen
https://unsplash.com/license

## Bug reports

Please report any and all bugs or problems that you find.  This is essential
for us to be able to improve this software!

https://github.com/zom/Zom-Android/issues


## Build Instructions

The project fully supports Gradle and Android Studio

### Get the source

The source code is all in the main git repos, with sub-projects setup as git
submodules:

    git clone https://github.com/zom/Zom-Android
    cd Zom-Android
    git submodule update --init


## Test Instructions

`mvn test`

See robo-tests/README.md for eclipse instructions.

Currently the instrumented target tests (to be run on a device) in the directory `tests` are empty.

## Logging

`adb -d logcat -v time -s Zom`

