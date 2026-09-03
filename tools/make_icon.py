"""Erzeugt die Grafiken des Mods im Terminal-Look von mods.ovitrinker.ch.

Geschrieben werden zwei Dateien aus derselben Zeichnung:
  src/main/resources/assets/oviclicker/icon.png   128x128, liegt im Jar
  branding/oviclicker-logo.png                    512x512, Projektbild fuer CurseForge

CurseForge verlangt fuer das Projektbild mindestens 400x400 px im Seitenverhaeltnis
1:1 als PNG, und es muss eine eigene Grafik sein - keine einfarbige Flaeche und kein
blosser Farbverlauf. Siehe
https://support.curseforge.com/support/solutions/articles/9000199552-project-submission-guide-and-tips

Farbregel der Seite: gruen = Text/Inhalt, hellblau (#5ad8ff) = alles Klickbare.
Deshalb ist der Cursor gruen und sind die Klick-Wellen hellblau.

Aufruf:  python tools/make_icon.py
"""

from pathlib import Path

from PIL import Image, ImageDraw

# Die gesamte Zeichnung ist in diesem Raster beschrieben und wird auf die
# jeweilige Ausgabegroesse hochgerechnet. So sehen Icon und Logo gleich aus.
GRID = 128

BACKGROUND = (5, 8, 6, 255)
GREEN = (61, 240, 122, 255)
GREEN_DARK = (16, 70, 38, 255)
BLUE = (90, 216, 255, 255)

# Spitze des Mauszeigers im Raster - von hier gehen die Klick-Wellen aus
TIP = (45, 52)

ROOT = Path(__file__).resolve().parent.parent
TARGETS = [
    (ROOT / "src/main/resources/assets/oviclicker/icon.png", 128),
    (ROOT / "branding/oviclicker-logo.png", 512),
]


class Canvas:
    """Zeichenflaeche, die im 128er-Raster rechnet und ueberabgetastet zeichnet."""

    def __init__(self, size: int, supersample: int) -> None:
        self.side = size * supersample
        self.scale = self.side / GRID
        self.image = Image.new("RGBA", (self.side, self.side), BACKGROUND)
        self.size = size

    def px(self, value: float) -> float:
        """Rechnet einen Wert im 128er-Raster auf die Zeichenflaeche um."""
        return value * self.scale

    def width(self, value: float) -> int:
        """Linienbreite im Raster, mindestens ein Pixel."""
        return max(1, int(round(self.px(value))))

    def draw(self) -> ImageDraw.ImageDraw:
        return ImageDraw.Draw(self.image)

    def finish(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        self.image.resize((self.size, self.size), Image.LANCZOS).save(path)


def draw_background(c: Canvas) -> None:
    """Schwarzer Grund mit angedeutetem Terminal-Raster."""
    d = c.draw()
    d.rectangle([0, 0, c.side, c.side], fill=BACKGROUND)
    for i in range(8, GRID, 16):
        d.line([(c.px(i), 0), (c.px(i), c.side)], fill=GREEN_DARK, width=c.width(0.5))
        d.line([(0, c.px(i)), (c.side, c.px(i))], fill=GREEN_DARK, width=c.width(0.5))


def draw_frame(c: Canvas) -> None:
    """Duenner gruener Rahmen mit Eckmarken, wie ein Terminalfenster."""
    d = c.draw()
    inset, corner = c.px(6), c.px(22)
    far = c.side - inset
    d.rectangle([inset, inset, far, far], outline=GREEN_DARK, width=c.width(1))
    for x0, y0, x1, y1 in (
        (inset, inset, inset + corner, inset),
        (inset, inset, inset, inset + corner),
        (far - corner, inset, far, inset),
        (far, inset, far, inset + corner),
        (inset, far, inset + corner, far),
        (inset, far - corner, inset, far),
        (far - corner, far, far, far),
        (far, far - corner, far, far),
    ):
        d.line([(x0, y0), (x1, y1)], fill=GREEN, width=c.width(1.5))


def draw_click_rings(c: Canvas) -> None:
    """Hellblaue Klick-Wellen, die von der Zeigerspitze nach oben rechts auslaufen."""
    tip = (c.px(TIP[0]), c.px(TIP[1]))
    for radius, line, alpha in ((21, 3.2, 245), (32, 2.6, 160), (43, 2.0, 85)):
        r = c.px(radius)
        layer = Image.new("RGBA", c.image.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).arc(
            [tip[0] - r, tip[1] - r, tip[0] + r, tip[1] + r],
            start=192,
            end=348,
            fill=BLUE[:3] + (alpha,),
            width=c.width(line),
        )
        c.image.alpha_composite(layer)


def draw_cursor(c: Canvas) -> None:
    """Klassischer Mauszeiger, Spitze oben links auf TIP."""
    shape = [(0, 0), (0, 72), (18, 56), (29, 86), (43, 80), (32, 51), (54, 50)]
    scale = 0.70
    points = [(c.px(TIP[0] + x * scale), c.px(TIP[1] + y * scale)) for x, y in shape]
    c.draw().polygon(points, fill=GREEN, outline=BACKGROUND[:3] + (255,), width=c.width(2))


def render(path: Path, size: int) -> None:
    # Ueberabtastung so waehlen, dass die Zeichenflaeche rund 1000 px breit ist
    c = Canvas(size, max(2, round(1024 / size)))
    draw_background(c)
    draw_frame(c)
    draw_click_rings(c)
    draw_cursor(c)
    c.finish(path)
    print("geschrieben: %s (%dx%d)" % (path, size, size))


def main() -> None:
    for path, size in TARGETS:
        render(path, size)


if __name__ == "__main__":
    main()
