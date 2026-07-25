# Replace unsalted MD5 password hashing with bcrypt

Passwords are stored as **unsalted MD5** (`users.passwort`, written by `Common.md5`, verified in
`DefaultAuthenticationManager`). We move to **bcrypt** via `org.springframework.security:spring-security-crypto`,
migrating existing accounts lazily: on successful login, verify against the legacy scheme, then
immediately rehash with bcrypt. After a stated window, remaining accounts are force-reset and the
legacy verification path is deleted.

bcrypt over argon2id because `BCryptPasswordEncoder` needs no transitive dependencies, while
`Argon2PasswordEncoder` pulls in BouncyCastle and exposes memory/parallelism parameters that have to
be tuned per host. bcrypt over hand-rolled PBKDF2 because salt generation and stored encoding are
precisely the things the current implementation got wrong. Both encoders work on Java 11 and 17, so
this does not block on ADR-0001.

## The three defects being fixed

**Unsalted.** `Common.md5(password)` is compared directly against the stored value. Identical
passwords produce identical hashes; rainbow tables apply. This is the most serious of the three and
the least visible.

**The hex encoding is lossy.** `Common.java:868` uses `Integer.toHexString(0xFF & b)` with no
zero-padding, so bytes below `0x10` render as one character instead of two. `md5("test123")` is
`cc03e747a6afbbcbf8be7668acfebee5` (32 chars) but DS stores `cc3e747a6afbbcbf8be7668acfebee5` (31) —
the `03` lost its zero. The output is not standard MD5, and distinct digests can collapse onto the
same string, shrinking the effective hash space.

**Generated passwords are predictable.** Registration and password reset both issue
`Common.md5(Integer.toString(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)))`. The result
looks like a 31-character hex string but carries the entropy of a single `int`, drawn from a PRNG
that is explicitly not cryptographically secure. Any account still on an auto-issued password sits in
a 2^31 search space against an unsalted hash. This is the urgent one, and it is fixable on its own —
switch to `SecureRandom` and emit a real random password — with no migration and no new dependency.
It is therefore sequenced ahead of the rest (roadmap 0.3 rather than 2.4).

## Consequences

**`Common.md5` is not changed.** It is also used to mint task IDs in `Task.java`, where the encoding
quirk is harmless. Correcting it in place would alter those IDs. Password hashing moves behind its own
abstraction; `Common.md5` stays as-is for non-password callers and should eventually be renamed to
reflect that it is not standard MD5.

**No schema change is needed for the hash itself.** `users.passwort` is `varchar(255)`; bcrypt output
is 60 characters. Legacy values are 31–32 lowercase hex characters and never contain `$`, while bcrypt
hashes always begin `$2a$`/`$2b$` — so the scheme in use is detectable from the value, and no
discriminator column is required.

**The legacy verification path is temporary by design.** It exists only to convert accounts on login
and must be deleted once stragglers are force-reset, otherwise MD5 verification lives in the codebase
permanently. Of 376 accounts, roughly 141 are recently active (`inakt < 50`), so the lazy path is
expected to convert well under half; the rest need the forced reset.

**Password reset depends on working email**, which the local Docker instance does not have and which
should be confirmed for production before the forced-reset step is triggered.
