"""Validate the user-supplied LG profile without changing or generating signals."""
from pathlib import Path
import hashlib
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT / 'app/src/main/assets/lg_60la6208_tv_ir_test.ir'


class LgProfileTests(unittest.TestCase):
    def test_original_upload_preserved(self):
        self.assertEqual(hashlib.sha256(ASSET.read_bytes()).hexdigest(), 'a8fa34a1c4f20fdfc3143eb258ac4ac6e30b6dfe0940cc8ad53073830321f9b8')

    def test_complete_valid_lg_frames(self):
        names = set()
        payloads = set()
        for block in re.split(r'(?m)^name: ', ASSET.read_text())[1:]:
            name = block.splitlines()[0]
            self.assertNotIn(name, names)
            names.add(name)
            fields = dict(line.split(': ', 1) for line in block.splitlines()[1:]
                          if ': ' in line and not line.startswith('#'))
            self.assertEqual(fields['type'], 'raw')
            self.assertEqual(int(fields['frequency']), 38000)
            data = list(map(int, fields['data'].split()))
            self.assertEqual(len(data), 67)
            self.assertTrue(all(t > 0 for t in data))
            self.assertLess(sum(data), 2_000_000)
            self.assertEqual(data[:2], [9000, 4500])
            self.assertTrue(all(mark == 560 for mark in data[2::2]))
            self.assertTrue(all(space in (560, 1690) for space in data[3::2]))
            payload = int(''.join('1' if t == 1690 else '0' for t in data[3::2]), 2)
            self.assertEqual(payload >> 16, 0x20DF)
            self.assertEqual(((payload >> 8) & 255) ^ (payload & 255), 255)
            self.assertNotIn(payload, payloads)
            payloads.add(payload)
        expected = {'POWER', 'INPUT', 'TV', 'HOME', 'SETTINGS', 'GUIDE', 'UP', 'DOWN',
                    'LEFT', 'RIGHT', 'OK', 'BACK', 'EXIT', 'INFO', 'VOL_PLUS', 'VOL_MINUS',
                    'MUTE', 'CHANNEL_PLUS', 'CHANNEL_MINUS'} | {f'NUM_{n}' for n in range(10)}
        self.assertEqual(names, expected)


if __name__ == '__main__':
    unittest.main()
