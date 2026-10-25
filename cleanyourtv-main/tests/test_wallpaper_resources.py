"""Wallpaper controls must remain readable in every language shipped by the launcher."""
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "launcher/app/src/main/res"


class WallpaperTranslationsTest(unittest.TestCase):
    def test_all_shipped_languages_cover_wallpaper_controls_and_format_arguments(self):
        def strings(path):
            return {node.attrib["name"]: node.text or "" for node in ET.parse(path).getroot()}

        default = strings(RES / "values/wallpaper_strings.xml")
        self.assertTrue(default)
        for folder in sorted(RES.glob("values*")):
            if not (folder / "strings.xml").is_file():
                continue
            with self.subTest(locale=folder.name):
                translated = strings(folder / "wallpaper_strings.xml")
                self.assertEqual(default.keys(), translated.keys())
                for key, value in default.items():
                    self.assertTrue(translated[key].strip(), key)
                    self.assertEqual(re.findall(r"%\d+\$[sd]|%%", value),
                                     re.findall(r"%\d+\$[sd]|%%", translated[key]), key)


if __name__ == "__main__":
    unittest.main()
