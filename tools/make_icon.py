"""Erzeugt das Mod-Icon (assets/oviclicker/icon.png) im Terminal-Look von mods.ovitrinker.ch.

Farbregel der Seite: gruen = Text/Inhalt, hellblau (#5ad8ff) = alles Klickbare.
Deshalb ist der Cursor gruen und der Klick-Ring hellblau.

Aufruf:  python tools/make_icon.py
"""

from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 128           # Endgroesse, von Fabric im Mod-Menue erwartet
SS = 8               # Supersampling-Faktor fuer weiche Kanten
S = SIZE * SS

BACKGROUND = (5, 8, 6, 255)
GRID = (28, 90, 48, 255)
GREEN = (61, 240, 122, 255)
GREEN_DARK = (16, 70, 38, 255)
BLUE = (90, 216, 255, 255)

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/oviclicker/icon.png"


def px(value: float) -> float:
    """Rechnet einen Wert im 128er-Raster auf die Supersampling-Groesse um."""
    return value * SS


def draw_background(draw: ImageDraw.ImageDraw) -> None:
    """Schwarzer Grund mit angedeutetem Terminal-Raster."""
    draw.rectangle([0, 0, S, S], fill=BACKGROUND)
    for i in range(8, SIZE, 16):
        draw.line([(px(i), 0), (px(i), S)], fill=GREEN_DARK, width=int(px(0.5)))
        draw.line([(0, px(i)), (S, px(i))], fill=GREEN_DARK, width=int(px(0.5)))


def draw_frame(draw: ImageDraw.ImageDraw) -> None:
    """Duenner gruener Rahmen mit Eckmarken, wie ein Terminalfenster."""
    inset, corner = px(6), px(22)
    draw.rectangle([inset, inset, S - inset, S - inset], outline=GREEN_DARK, width=int(px(1)))
    for x0, y0, x1, y1 in (
        (inset, inset, inset + corner, inset),
        (inset, inset, inset, inset + corner),
        (S - inset - corner, inset, S - inset, inset),
        (S - inset, inset, S - inset, inset + corner),
        (inset, S - inset, inset + corner, S - inset),
        (inset, S - inset - corner, inset, S - inset),
        (S - inset - corner, S - inset, S - inset, S - inset),
        (S - inset, S - inset - corner, S - inset, S - inset),
    ):
        draw.line([(x0, y0), (x1, y1)], fill=GREEN, width=int(px(1.5)))


# Spitze des Mauszeigers - von hier gehen die Klick-Wellen aus
TIP = (px(45), px(52))


def draw_click_rings(image: Image.Image) -> None:
    """Hellblaue Klick-Wellen, die von der Zeigerspitze nach oben rechts auslaufen."""
    for radius, width, alpha in ((px(21), px(3.2), 245), (px(32), px(2.6), 160), (px(43), px(2.0), 85)):
        layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
        ImageDraw.Draw(layer).arc(
            [TIP[0] - radius, TIP[1] - radius, TIP[0] + radius, TIP[1] + radius],
            start=192,
            end=348,
            fill=BLUE[:3] + (alpha,),
            width=int(width),
        )
        image.alpha_composite(layer)


def draw_cursor(draw: ImageDraw.ImageDraw) -> None:
    """Klassischer Mauszeiger, Spitze oben links."""
    # Umriss im 100er-Raster, danach skaliert und verschoben
    shape = [(0, 0), (0, 72), (18, 56), (29, 86), (43, 80), (32, 51), (54, 50)]
    scale = 0.70
    points = [(TIP[0] + px(x * scale), TIP[1] + px(y * scale)) for x, y in shape]
    draw.polygon(points, fill=GREEN, outline=BACKGROUND[:3] + (255,), width=int(px(2)))


def main() -> None:
    image = Image.new("RGBA", (S, S), BACKGROUND)
    draw = ImageDraw.Draw(image)

    draw_background(draw)
    draw_frame(draw)
    draw_click_rings(image)
    draw_cursor(ImageDraw.Draw(image))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    image.resize((SIZE, SIZE), Image.LANCZOS).save(OUT)
    print(f"geschrieben: {OUT}")


if __name__ == "__main__":
    main()
