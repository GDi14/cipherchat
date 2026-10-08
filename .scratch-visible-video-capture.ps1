param(
    [string]$InputVideo = 'E:\OneDrive\Desktop\Screen_recording_20261007_100659.mp4',
    [string]$OutputDirectory = 'E:\CCv2\.scratch-video-frames'
)

Add-Type -AssemblyName PresentationCore
Add-Type -AssemblyName PresentationFramework
Add-Type -AssemblyName WindowsBase
Add-Type -AssemblyName System.Drawing
$null = [System.IO.Directory]::CreateDirectory($OutputDirectory)

$window = New-Object System.Windows.Window
$window.Title = 'Recording frame capture'
$window.Width = 405
$window.Height = 720
$window.Left = 80
$window.Top = 60
$window.WindowStyle = [System.Windows.WindowStyle]::None
$window.ResizeMode = [System.Windows.ResizeMode]::NoResize
$window.Topmost = $true
$window.Background = [System.Windows.Media.Brushes]::Black

$media = New-Object System.Windows.Controls.MediaElement
$media.LoadedBehavior = [System.Windows.Controls.MediaState]::Manual
$media.UnloadedBehavior = [System.Windows.Controls.MediaState]::Manual
$media.Stretch = [System.Windows.Media.Stretch]::Uniform
$media.Source = [Uri]::new($InputVideo)
$window.Content = $media

$script:index = 0
$script:frameCount = 48
$script:captureTimer = $null
$media.Add_MediaOpened({
    $media.Pause()
    $script:captureTimer = New-Object System.Windows.Threading.DispatcherTimer
    $script:captureTimer.Interval = [TimeSpan]::FromMilliseconds(180)
    $script:captureTimer.Add_Tick({
        if ($script:index -ge $script:frameCount) {
            $script:captureTimer.Stop()
            $window.Close()
            return
        }
        $time = 6.0 * $script:index / ($script:frameCount - 1)
        $media.Position = [TimeSpan]::FromSeconds($time)
        $captureOnce = New-Object System.Windows.Threading.DispatcherTimer
        $captureOnce.Interval = [TimeSpan]::FromMilliseconds(90)
        $captureOnce.Add_Tick({
            $captureOnce.Stop()
            $origin = $window.PointToScreen([System.Windows.Point]::new(0, 0))
            $bitmap = New-Object System.Drawing.Bitmap(405, 720)
            $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
            $graphics.CopyFromScreen([int]$origin.X, [int]$origin.Y, 0, 0, $bitmap.Size)
            $bitmap.Save((Join-Path $OutputDirectory ('screen-{0:D3}.png' -f $script:index)), [System.Drawing.Imaging.ImageFormat]::Png)
            $graphics.Dispose()
            $bitmap.Dispose()
            $script:index++
        })
        $captureOnce.Start()
    })
    $script:captureTimer.Start()
})
$media.Add_MediaFailed({ param($sender, $eventArgs) Write-Error $eventArgs.ErrorException.Message; $window.Close() })
$window.ShowDialog() | Out-Null
Write-Output "Captured=$script:index Frames=$OutputDirectory"
