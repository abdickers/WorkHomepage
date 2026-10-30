param([Parameter(Mandatory=$true)][string]$Python)
$ErrorActionPreference = 'Stop'
# Restricted to this repository. No credentials are written to source or output.
$repository = 'abdickers/WorkHomepage'
$backup = Join-Path $PSScriptRoot '../outputs/signing/command-signing.dpapi'
New-Item -ItemType Directory -Force (Split-Path $backup) | Out-Null
Add-Type -AssemblyName System.Security.Cryptography.ProtectedData
if (Test-Path -LiteralPath $backup) {
    $protected = [IO.File]::ReadAllBytes($backup)
    $materialJson = [Text.Encoding]::UTF8.GetString([Security.Cryptography.ProtectedData]::Unprotect($protected,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))
} else {
    $create = @'
import json,secrets,base64,datetime
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
key=rsa.generate_private_key(public_exponent=65537,key_size=3072)
name=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'Middle-earth Command')])
now=datetime.datetime.now(datetime.timezone.utc)
certificate=(x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(days=1)).not_valid_after(now+datetime.timedelta(days=365*30)).sign(key,hashes.SHA256()))
password=secrets.token_urlsafe(36)
p12=pkcs12.serialize_key_and_certificates(b'command',key,certificate,None,serialization.BestAvailableEncryption(password.encode()))
print(json.dumps({'keystore':base64.b64encode(p12).decode(),'password':password,'sha1':certificate.fingerprint(hashes.SHA1()).hex().upper(),'sha256':certificate.fingerprint(hashes.SHA256()).hex().upper()}))
'@
    $materialJson = & $Python -c $create
    if ($LASTEXITCODE -ne 0) {throw 'Could not generate app signing material'}
    [IO.File]::WriteAllBytes($backup,[Security.Cryptography.ProtectedData]::Protect([Text.Encoding]::UTF8.GetBytes($materialJson),$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))
}
$material = $materialJson | ConvertFrom-Json
$credentials = @{}
try {
    $credentialOutput = "protocol=https`nhost=github.com`nusername=abdickers`npath=$repository.git`n`n" | git credential fill
    foreach($line in $credentialOutput){$pair=$line -split '=',2;if($pair.Count -eq 2){$credentials[$pair[0]]=$pair[1]}}
    if(-not $credentials['password']){throw 'The repository credential helper returned no credential'}
    $headers = @{Authorization=('Bearer '+$credentials['password']);Accept='application/vnd.github+json';'X-GitHub-Api-Version'='2022-11-28'}
    $api = "https://api.github.com/repos/$repository/actions/secrets"
    $publicKey = Invoke-RestMethod -Uri "$api/public-key" -Headers $headers
    $seal = @'
import sys,json,base64
sys.path.insert(0,'outputs/signing-tools')
from nacl.public import PublicKey,SealedBox
data=json.load(sys.stdin)
box=SealedBox(PublicKey(base64.b64decode(data['publicKey'])))
print(json.dumps({name:base64.b64encode(box.encrypt(value.encode())).decode() for name,value in data['values'].items()}))
'@
    $inputJson = @{publicKey=$publicKey.key;values=@{COMMAND_KEYSTORE_BASE64=$material.keystore;COMMAND_KEYSTORE_PASSWORD=$material.password}} | ConvertTo-Json -Depth 5 -Compress
    $sealedJson = $inputJson | & $Python -c $seal
    if($LASTEXITCODE -ne 0){throw 'Could not encrypt repository signing secrets'}
    $sealed = $sealedJson | ConvertFrom-Json
    foreach($name in @('COMMAND_KEYSTORE_BASE64','COMMAND_KEYSTORE_PASSWORD')) {
        $body = @{encrypted_value=$sealed.$name;key_id=$publicKey.key_id} | ConvertTo-Json -Compress
        Invoke-RestMethod -Method Put -Uri "$api/$name" -Headers $headers -ContentType 'application/json' -Body $body | Out-Null
    }
    $public = @{package='com.abdickers.realms';alias='command';sha1=$material.sha1;sha256=$material.sha256}
    $public | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $PSScriptRoot '../android-command/signing-certificate.json') -Encoding utf8
    Write-Output 'Stable signing key saved as encrypted GitHub Actions secrets. Local recovery copy is Windows-user encrypted.'
    Write-Output ('Public certificate SHA-256: '+$material.sha256)
} finally {
    $credentialOutput=$null;$materialJson=$null;$material=$null;$inputJson=$null;$credentials.Clear();$headers=$null
}
