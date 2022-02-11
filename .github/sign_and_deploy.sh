#!/usr/bin/env bash
KEY_FILE='daniel.flassak.open.source.private.key'

echo $GPG_KEY_BASE64 | base64 -d  > ${KEY_FILE}
gpg --passphrase "${GPG_PASSPHRASE}" --batch --yes --fast-import ${KEY_FILE}

echo gpg keyname ${GPG_KEYNAME}

if [[ "${REF_TYPE}" == "tag" ]]; then
    # -P sign plugin is used instead of gpg:sign because gpg:sign has side-effects
    # also, install cannot be used because deploy will cause the signatures to be invalid because it
    # re-creates the jars. So explicitly calling source:jar and javadoc:jar seems to be the only
    # viable solution.
    mvn --batch-mode -DskipTests=true -Dproject.version=${REF_NAME} -P sign clean source:jar javadoc:jar deploy
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
