#!/usr/bin/env bash
KEY_FILE='daniel.flassak.open.source.private.key'

echo $GPG_KEY_BASE64 | base64 -d  > ${KEY_FILE}
gpg --passphrase "${GPG_PASSPHRASE}" --batch --yes --fast-import ${KEY_FILE}

echo gpg keyname ${GPG_KEYNAME}

if [[ "${REF_TYPE}" == "tag" ]]; then
    mvn --batch-mode install -P sign -DskipTests=true -Dproject.version=${REF_NAME}
    SUCCESS=$?
else
    echo "this should only be run for tags"
    SUCCESS=1
fi

tree target # TODO: remove

# just to be safe, although this deleting these should not be necessary
rm ${KEY_FILE}
rm -rf ~/.gnupg

exit ${SUCCESS}
