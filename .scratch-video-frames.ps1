param(
    [string]$InputVideo = 'E:\OneDrive\Desktop\Screen_recording_20261007_100659.mp4',
    [string]$OutputDirectory = 'E:\CCv2\.scratch-video-frames'
)

Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName WindowsBase
Add-Type -AssemblyName PresentationFramework
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$player = New-Object System.Windows.Media.MediaPlayer
$player.Open([Uri]::new($InputVideo))
$deadline = [DateTime]::UtcNow.AddSeconds(10)
while ($player.NaturalVideoWidth -eq 0 -and [DateTime]::UtcNow -lt $deadline) {
    [System.Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Action]{}, [System.Windows.Threading.DispatcherPriority]::Background)
    Start-Sleep -Milliseconds 30
}
if ($player.NaturalVideoWidth -eq 0) { throw 'Video did not open in MediaPlayer.' }
$duration = $player.NaturalDuration.TimeSpan.TotalSeconds
$width = $player.NaturalVideoWidth
$height = $player.NaturalVideoHeight
$count = [Math]::Min(60, [Math]::Max(1, [Math]::Ceiling($duration * 8)))
for ($i = 0; $i -lt $count; $i++) {
    $time = if ($count -eq 1) { 0 } else { $duration * $i / ($count - 1) }
    $player.Position = [TimeSpan]::FromSeconds($time)
    [System.Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Action]{}, [System.Windows.Threading.DispatcherPriority]::Background)
    Start-Sleep -Milliseconds 80
    $visual = New-Object System.Windows.Media.DrawingVisual
    $context = $visual.RenderOpen()
    $context.DrawVideo($player, [System.Windows.Rect]::new(0, 0, $width, $height))
    $context.Close()
    $bitmap = New-Object System.Windows.Media.Imaging.RenderTargetBitmap($width, $height, 96, 96, [System.Windows.Media.PixelFormats]::Pbgra32)
    $bitmap.Render($visual)
    $encoder = New-Object System.Windows.Media.Imaging.PngBitmapEncoder
    $encoder.Frames.Add([System.Windows.Media.Imaging.BitmapFrame]::Create($bitmap))
    $stream = [System.IO.File]::Create((Join-Path $OutputDirectory ('frame-{0:D3}.png' -f $i)))
    $encoder.Save($stream)
    $stream.Dispose()
}
$player.Close()
Write-Output "Frames=$count Duration=$duration Size=${width}x${height} Output=$OutputDirectory"
