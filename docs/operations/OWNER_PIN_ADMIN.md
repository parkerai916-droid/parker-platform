# Owner PIN host administration

The only supported human-facing PIN administration path is the direct host
tool at tools/owner-pin-admin.sh. It is not an HTTP endpoint and is not
exposed to the Owner browser.

Operations:

    tools/owner-pin-admin.sh set
    tools/owner-pin-admin.sh change
    tools/owner-pin-admin.sh reset
    tools/owner-pin-admin.sh verify-status

The default hash target is:

    /mnt/parker-secrets/parker/owner-high-authority-pin.hash

The existing recovery credential remains at:

    /mnt/parker-secrets/parker/owner-high-authority-verification.secret

set is permitted only when no PIN hash exists. change requires a valid current
hash and reset may repair a missing or invalid hash; both require the existing
recovery credential before accepting a replacement PIN. Every PIN and
confirmation is entered through hidden terminal input; values are never
accepted as command-line arguments, environment variables, or standard input
from a pipe.

The tool writes only an Argon2id PHC record using 32 MiB memory, three
iterations, one lane, a 16-byte random salt, and a 32-byte derived hash. It
writes a same-directory temporary file, sets the effective runtime-readable
root:root 0440 policy, copies an existing access ACL when replacing a hash,
and explicitly grants Parker UID 999 read-only access through the host POSIX
ACL (`u:999:r--`) before the atomic replacement. The required host tools are
`/usr/bin/getfacl` and `/usr/bin/setfacl`; if the access model cannot be
established, the old target is preserved and the operation fails closed. The
tool fsyncs the file and attempts to fsync the directory. It rejects symlink
targets and does not create backups or modify the long recovery secret.

verify-status reports only whether the target is configured and whether its
Argon2id record is valid. It never prints the hash or any credential value.

Run canonical operations with the required host administrative privilege. The
host mode alone does not grant UID/GID 999 access on every deployment: Parker's
runtime must preserve the established Compose UID/GID/mode mapping or the
deployment's existing ACL grant. Unit 6 must verify the mounted file is
readable by UID/GID 999 and read-only after restart. Tests use isolated
temporary files only.
