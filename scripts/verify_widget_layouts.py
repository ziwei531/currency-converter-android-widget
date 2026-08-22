#!/usr/bin/env python3
"""Fail the build if responsive widget roots drift visually."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ElementTree

ANDROID_NAMESPACE = "{http://schemas.android.com/apk/res/android}"
EXPECTED_PADDING = "@dimen/widget_content_inset"
EXPECTED_BACKGROUND = "@drawable/widget_bg"
LAYOUTS = (
    "widget_rate.xml",
    "widget_rate_compact.xml",
    "widget_rate_expanded.xml",
)


def verify_layout( layout_path: Path ) -> list[str]:
    root = ElementTree.parse( layout_path ).getroot()
    padding = root.get( ANDROID_NAMESPACE + "padding" )
    background = root.get( ANDROID_NAMESPACE + "background" )
    failures = []
    if padding != EXPECTED_PADDING:
        failures.append( f"{layout_path}: root padding is {padding!r}, expected {EXPECTED_PADDING!r}" )
    if background != EXPECTED_BACKGROUND:
        failures.append( f"{layout_path}: root background is {background!r}, expected {EXPECTED_BACKGROUND!r}" )
    return failures


def main() -> int:
    root = Path( __file__ ).resolve().parent.parent
    layout_directory = root / "res" / "layout"
    failures = []
    for layout_name in LAYOUTS:
        failures.extend( verify_layout( layout_directory / layout_name ) )
    if failures:
        print( "Responsive widget layout verification failed:", file=sys.stderr )
        print( "\n".join( failures ), file=sys.stderr )
        return 1
    print( "Responsive widget layout verification passed." )
    return 0


if __name__ == "__main__":
    raise SystemExit( main() )
