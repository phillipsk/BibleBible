#!/usr/bin/env python3
"""Summarize the current Gradle JUnit XML reports for a CI job."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

print('| Suite | Executed | Failed | Errors | Skipped |')
print('| --- | ---: | ---: | ---: | ---: |')
missing = False
for directory in sys.argv[1:]:
    folder = Path(directory)
    reports = list(folder.glob('TEST-*.xml'))
    totals = dict.fromkeys(('tests', 'failures', 'errors', 'skipped'), 0)
    for report in reports:
        root = ET.parse(report).getroot()
        for key in totals:
            totals[key] += int(root.get(key, '0'))
    executed = totals['tests'] - totals['skipped']
    print(f"| {folder.name} | {executed} | {totals['failures']} | {totals['errors']} | {totals['skipped']} |")
    if executed == 0:
        print(f'\nNo completed tests were reported for `{folder}`.\n')
        missing = True
if missing:
    raise SystemExit(1)
