#!/usr/bin/env bash
echo $GPG_KEY_BASE64 | base64 -d  > daniel.flassak.open.source.private.key
gpg --passphrase "${GPG_PASSPHRASE}" --batch --yes --fast-import daniel.flassak.open.source.private.key

# TODO: only deploy for tags on master
# TODO: where does the version come from here?
# TODO: maven batch mode everywhere
echo gpg keyname $GPG_KEYNAME

mvn --batch-mode install -P sign --settings .github/mvnsettings.xml -DskipTests=true

tree target

# just to be safe, although this should not be necessary
rm daniel.flassak.open.source.private.key
rm -rf ~/.gnupg
