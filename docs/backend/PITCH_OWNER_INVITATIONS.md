# Pitch Owner Invitation Codes

Pitch Owner capability is invite-only for the initial V1 backend foundation.
Supabase Auth still owns authentication. Peladinhas stores only the local user
profile, the owner capability row, and hashed invitation codes.

## Safety Rules

- Never store a plaintext invitation code in Git, documentation, logs, tickets, or chat.
- Generate codes with high entropy, share the plaintext code only with the intended owner, and insert only its SHA-256 hash into PostgreSQL.
- Invitation codes are single-use. Once redeemed, `used_at` and `used_by_user_id` are populated atomically by the backend.
- Expiry is optional. Use `expires_at` when an invitation should stop working after a known time.
- There is intentionally no public API endpoint for generating invitation codes.

## Generate a Local Code and Hash

PowerShell example for an operator workstation:

```powershell
$bytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$code = [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
$hashBytes = [System.Security.Cryptography.SHA256]::HashData([System.Text.Encoding]::UTF8.GetBytes($code))
$hash = -join ($hashBytes | ForEach-Object { $_.ToString('x2') })

"Plaintext invitation code, share once with the owner: $code"
"Hash to insert into PostgreSQL: $hash"
```

## Insert the Hash

Insert only the hash into the hosted PostgreSQL database. Use a new random UUID
for `id`. The example uses placeholders only.

```sql
insert into pitch_owner_invitation_codes (
    id,
    code_hash,
    expires_at,
    created_at
)
values (
    '<generated-uuid>',
    '<sha256-hash-only>',
    '<optional-expiry-timestamptz-or-null>',
    current_timestamp
);
```

After insertion, the owner can choose Pitch Owner onboarding or activate Pitch
Owner capability later from settings by submitting the plaintext invitation code.
