"""Regression checks for the supplied DStv digit captures; no waveform generation."""
from pathlib import Path
import re
import unittest

ASSET = Path(__file__).resolve().parents[1] / "app/src/main/assets/dstv_8s_final_working.ir"


class DigitSignalTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.digits = {}
        for block in re.split(r"(?m)^name: ", ASSET.read_text())[1:]:
            name = block.splitlines()[0].strip()
            if not name.startswith("NUM_"):
                continue
            if name in cls.digits:
                raise AssertionError(f"Duplicate IR command: {name}")
            cls.digits[name] = list(map(int, re.search(r"(?m)^data: (.+)$", block)[1].split()))

    def test_every_digit_has_a_signal(self):
        self.assertEqual(set(self.digits), {f"NUM_{n}" for n in range(10)})

    def test_digit_frames_have_32_bits(self):
        # Two leader timings + 32 mark/space pairs + one final mark.
        # The old NUM_1 accidentally had an extra pair and passed runtime validation.
        for name, timings in self.digits.items():
            with self.subTest(command=name):
                self.assertEqual(len(timings), 67)
                self.assertTrue(all(t > 0 for t in timings))
                self.assertLess(sum(timings), 2_000_000)

    def test_digit_payload_matches_button(self):
        # Decode the supplied pulse-distance frames to catch duplicate/wrong digits.
        payloads = set()
        for number in range(10):
            with self.subTest(digit=number):
                timings = self.digits[f"NUM_{number}"]
                bits = "".join("1" if space > 1000 else "0" for space in timings[3::2])
                self.assertEqual(len(bits), 32)
                self.assertEqual(int(bits[:16], 2), 0x6F90)
                self.assertEqual(int(bits[16:20], 2), number)
                self.assertEqual(int(bits[20:24], 2), 0xF)
                self.assertEqual(int(bits[24:28], 2), number ^ 0xF)
                self.assertEqual(int(bits[28:], 2), 0)
                payloads.add(bits)
        self.assertEqual(len(payloads), 10)


if __name__ == "__main__":
    unittest.main()
