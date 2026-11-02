"""Keep the uploaded working profiles intact while adding the six tested captures."""
from pathlib import Path
import hashlib
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


def signals(text):
    result = {}
    for block in re.split(r'(?m)^name: ', text)[1:]:
        name = block.splitlines()[0]
        if name in result:
            raise AssertionError(f'Duplicate command: {name}')
        fields = dict(line.split(': ', 1) for line in block.splitlines()[1:]
                      if ': ' in line and not line.startswith('#'))
        result[name] = fields
    return result


class AddedCommandTests(unittest.TestCase):
    def test_original_dstv_upload_is_unchanged(self):
        data = (ROOT / 'app/src/main/assets/dstv_8s_final_working.ir').read_bytes()
        self.assertEqual(hashlib.sha256(data[:7328]).hexdigest(),
                         '2eb896eb3ba986e8555d3148c4c58f1c9c1174a354f1a93c6b23cf8a55917b86')
        self.assertEqual(len(signals(data.decode())), 26)

    def test_added_captures_are_preserved_and_distinct(self):
        commands = signals((ROOT / 'app/src/main/assets/dstv_8s_final_working.ir').read_text())
        expected = {
            'POWER': '816b19334306eba03fa910a27e21e9dceb705072315e85a7fc9801c58f3e8f26',
            'DSTV_MENU': '72e1fcd960aec9c66cf42044a8f11488570aa8c9284952d295745940e02fe005',
            'TV_GUIDE': '125703e5ceb6ef867e401705c2556a41047c059390f49ab7a8d4771839d6b37d',
            'OPTIONS': '61cd1eadbdc1a8c8ed261094a8b078e4d236429a5e4354c4bf37a4f584007403',
            'CHANNEL_UP': '10a5c51ef2f2a77825966257f89d94e22751023b6da7814b6c7149441fa28bb3',
            'CHANNEL_DOWN': '591406b807d714ccd7a2abf01ad02f7c53e642b436db5dd4f4520b5d5140aeef',
        }
        payloads = set()
        for name in ['POWER', 'DSTV_MENU', 'TV_GUIDE', 'OPTIONS', 'CHANNEL_UP', 'CHANNEL_DOWN']:
            with self.subTest(command=name):
                fields = commands[name]
                self.assertEqual(fields['type'], 'raw')
                self.assertEqual(int(fields['frequency']), 38029)
                values = tuple(map(int, fields['data'].split()))
                self.assertEqual(hashlib.sha256(fields['data'].encode()).hexdigest(), expected[name])
                self.assertTrue(all(t > 0 for t in values))
                self.assertLess(sum(values), 2_000_000)
                self.assertNotIn(values, payloads)
                payloads.add(values)
