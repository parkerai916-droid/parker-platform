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

set is permitted only when no PIN hash exists. change and reset require the
existing recovery credential before accepting a replacement PIN. Every PIN and
confirmation is entered through hidden terminal input; values are never
accepted as command-line arguments, environment variables, or standard input
from a pipe.

The tool writes only an Argon2id PHC record using 32 MiB memory, three
iterations, one lane, a 16-byte random salt, and a 32-byte derived hash. It
writes a same-directory temporary file, sets the effective runtime-readable
root:root 0440 policy, fsyncs, atomically replaces the target, and fsyncs the
directory. It rejects symlink targets and does not create backups or modify
the long recovery secret.

verify-status reports only whether the target is configured and whether its
Argon2id record is valid. It never prints the hash or any credential value.

Run canonical operations with the required host administrative privilege so
the runtime UID/GID 999:999 can read the effective root:root 0440 file through
the existing secret mount model. Tests use isolated temporary files only.
