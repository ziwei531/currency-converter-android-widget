#!/usr/bin/env python3
"""Fail the build if responsive widget card geometry drifts."""

from pathlib import Path
import sys
import xml.etree.ElementTree as ElementTree

ANDROID_NAMESPACE = "{http://schemas.android.com/apk/res/android}"
EXPECTED_CARD_MARGIN = "@dimen/widget_card_inset"
EXPECTED_CARD_PADDING = "@dimen/widget_content_inset"
EXPECTED_CARD_BACKGROUND = "@drawable/widget_bg"
LAYOUTS = (
    "widget_rate.xml",
    "widget_rate_compact.xml",
    "widget_rate_expanded.xml",
)


def verify_layout( layout_path: Path ) -> list[str]:
    root = ElementTree.parse( layout_path ).getroot()
    failures = []
    if root.tag != "FrameLayout":
        failures.append( f"{layout_path}: root must be FrameLayout, found {root.tag}" )
        return failures

    card = next(
        (
            child
            for child in root
            if child.get( ANDROID_NAMESPACE + "id" ) == "@+id/widget_card"
        ),
        None,
    )
    if card is None:
        failures.append( f"{layout_path}: missing widget_card child" )
        return failures

    checks = (
        ( "layout_margin", EXPECTED_CARD_MARGIN ),
        ( "padding", EXPECTED_CARD_PADDING ),
        ( "background", EXPECTED_CARD_BACKGROUND ),
    )
    for attribute, expected in checks:
        actual = card.get( ANDROID_NAMESPACE + attribute )
        if actual != expected:
            failures.append( f"{layout_path}: widget_card {attribute} is {actual!r}, expected {expected!r}" )
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
    print( "Responsive widget card geometry verification passed." )
    return 0


if __name__ == "__main__":
    raise SystemExit( main() )
