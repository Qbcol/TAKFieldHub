from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
XAML = ROOT / 'apps/builder/FieldTakHub.Builder/MainWindow.xaml'
s = XAML.read_text(encoding='utf-8')
assert s.count('<ScrollViewer') >= 1, 'Expected outer app ScrollViewer plus status/details ScrollViewer'
assert 'HorizontalScrollBarVisibility="Auto" VerticalScrollBarVisibility="Auto"' in s
assert 'MinWidth="1180" MinHeight="760"' in s
assert 'CanContentScroll="False"' in s
# The left pane must no longer own the app-level vertical scrolling.
assert '<ScrollViewer Grid.Column="0"' not in s
assert '<StackPanel Grid.Column="0" Grid.Row="0" Grid.RowSpan="2"' in s
print('BUILDER FULL-APP SCROLL CONTRACT: PASS')
