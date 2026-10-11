# Requires PowerShell 7. Run against the isolated compose.verify.yml stack only.
param([string]$BaseUrl = 'http://127.0.0.1:18080', [string]$SnapshotPath = '', [switch]$AfterRestart)
$ErrorActionPreference = 'Stop'
function Call-Api([string]$Method, [string]$Path, [int]$Expected, $Body = $null, [string]$Token = '') {
    $args = @{ Uri = "$BaseUrl$Path"; Method = $Method; SkipHttpErrorCheck = $true; TimeoutSec = 15 }
    if ($null -ne $Body) { $args.ContentType = 'application/json'; $args.Body = $Body | ConvertTo-Json -Depth 8 }
    if ($Token) { $args.Headers = @{Authorization = "Bearer $Token"} }
    $response = Invoke-WebRequest @args
    if ([int]$response.StatusCode -ne $Expected) { throw "$Method $Path expected $Expected, received $($response.StatusCode)" }
    if ($response.Content) { return $response.Content | ConvertFrom-Json }
}
$ready = $false
for ($attempt = 0; $attempt -lt 120; $attempt++) {
    try {
        # The HTTP server may accept requests before CommandLineRunner commits the seed transaction.
        $probe = Call-Api GET '/api/venues' 200
        if ($probe.totalElements -ge 2) { $ready = $true; break }
    } catch { }
    Start-Sleep -Seconds 1
}
if (!$ready) { throw 'Backend did not become ready' }
function Get-Snapshot {
    $v = Call-Api GET '/api/venues?size=100' 200
    $c = Call-Api GET '/api/courts?size=100' 200
    $ids = @($c.content | ForEach-Object { $_.id } | Sort-Object)
    $scheduleIds = @()
    foreach ($id in $ids) {
        $scheduleIds += @(Call-Api GET "/api/courts/$id/schedules" 200 | ForEach-Object { $_.id })
    }
    return @{venues = @($v.content | ForEach-Object { $_.id } | Sort-Object); courts = $ids; schedules = @($scheduleIds | Sort-Object)}
}
$before = Get-Snapshot
if ($AfterRestart) {
    if (!$SnapshotPath -or !(Test-Path $SnapshotPath)) { throw 'SnapshotPath is required after restart' }
    $previous = Get-Content -Raw $SnapshotPath | ConvertFrom-Json -AsHashtable
    foreach ($key in @('venues','courts','schedules')) {
        if (($previous[$key] -join ',') -ne ($before[$key] -join ',')) { throw "Changed $key after restart" }
    }
    Write-Output 'PASS restart: same venue, court and schedule IDs; no duplicated seed data'
    exit 0
}
if ($before.venues.Count -ne 2 -or $before.courts.Count -ne 3 -or $before.schedules.Count -ne 21) {
    throw 'Expected a fresh isolated database with 2 venues, 3 courts and 21 schedules'
}
$api = Call-Api GET '/v3/api-docs' 200
if (!$api.paths.'/api/venues' -or !$api.paths.'/api/courts' -or !$api.components.securitySchemes.'Bearer Authentication') {
    throw 'OpenAPI paths or bearer scheme missing'
}
$null = Call-Api POST '/api/owner/venues' 401 @{name='Smoke';address='Street'}
$null = Call-Api GET '/api/owner/venues' 401 $null 'invalid.jwt.token'
$userLogin = Call-Api POST '/api/auth/login' 200 @{username='user01';password='12345678'}
$null = Call-Api POST '/api/owner/venues' 403 @{name='Smoke';address='Street'} $userLogin.accessToken
# Preserve develop's observability and authorization behavior after integrating the venue routes.
$null = Call-Api GET '/actuator/health' 200
$null = Call-Api GET '/actuator/metrics' 401
$null = Call-Api GET '/actuator/metrics' 403 $null $userLogin.accessToken
$adminLogin = Call-Api POST '/api/auth/login' 200 @{username='admin01';password='12345678'}
$null = Call-Api GET '/actuator/metrics' 200 $null $adminLogin.accessToken
$traced = Invoke-WebRequest "$BaseUrl/api/venues" -Headers @{'X-Request-Id'='venue-court-smoke'}
if ($traced.Headers['X-Request-Id'] -ne 'venue-court-smoke') { throw 'Request tracing header was lost' }
$ownerLogin = Call-Api POST '/api/auth/login' 200 @{username='owner01';password='12345678'}
$token = $ownerLogin.accessToken
$null = Call-Api POST '/api/owner/venues' 400 @{} $token
$null = Call-Api GET '/api/venues/99999999' 404
$null = Call-Api GET '/api/courts/99999999' 404
$null = Call-Api GET '/api/courts?minPrice=200&maxPrice=100' 400
$null = Call-Api GET '/api/venues?sortBy=unknown' 400
$venueBody = @{name='Smoke verification';address='Test Street';city='Test City';district='Test District'}
$venue = Call-Api POST '/api/owner/venues' 201 $venueBody $token
$null = Call-Api POST '/api/owner/venues' 409 $venueBody $token
$sports = Call-Api GET '/api/sports?size=100' 200
$sportId = $sports.content[0].id
$courtBody = @{name='Smoke court';sportId=$sportId;pricePerHour=120;openTime='08:00';closeTime='20:00'}
$courtPath = "/api/owner/venues/$($venue.id)/courts"
$null = Call-Api POST $courtPath 400 @{name='Invalid';sportId=$sportId;pricePerHour=-1} $token
$court = Call-Api POST $courtPath 201 $courtBody $token
$null = Call-Api POST $courtPath 409 $courtBody $token
$search = Call-Api GET "/api/courts?keyword=Smoke&sportId=$sportId&venueId=$($venue.id)&minPrice=120&maxPrice=120&size=1&sortBy=pricePerHour&direction=desc" 200
if ($search.totalElements -ne 1 -or $search.content[0].id -ne $court.id) { throw 'Combined court search mismatch' }
$venueSearch = Call-Api GET "/api/venues?keyword=Smoke&city=Test%20City&district=Test%20District&ownerId=$($ownerLogin.userId)&size=1" 200
if ($venueSearch.totalElements -ne 1) { throw 'Combined venue search mismatch' }
$courtBody.pricePerHour = 150
$null = Call-Api PUT "$courtPath/$($court.id)" 200 $courtBody $token
$scheduleBody = @{dayOfWeek='MONDAY';openTime='08:00';closeTime='20:00';available=$true}
$schedulePath = "$courtPath/$($court.id)/schedules"
$schedule = Call-Api POST $schedulePath 201 $scheduleBody $token
$null = Call-Api POST $schedulePath 409 $scheduleBody $token
$scheduleBody.dayOfWeek = 'TUESDAY'
$null = Call-Api PUT "$schedulePath/$($schedule.id)" 200 $scheduleBody $token
$null = Call-Api DELETE "$schedulePath/$($schedule.id)" 200 $null $token
$null = Call-Api DELETE "$courtPath/$($court.id)" 200 $null $token
$null = Call-Api DELETE "/api/owner/venues/$($venue.id)" 200 $null $token
$after = Get-Snapshot
foreach ($key in @('venues','courts','schedules')) {
    if (($before[$key] -join ',') -ne ($after[$key] -join ',')) { throw "Smoke CRUD left unexpected $key" }
}
if ($SnapshotPath) { $after | ConvertTo-Json | Set-Content $SnapshotPath }
Write-Output 'PASS MySQL HTTP smoke: seed 2/3/21, JWT, Swagger, 400/401/403/404/409, CRUD, combined filters, actuator authorization, request tracing and cleanup'
