"""Regenera os assets Android do ícone oficial sem alterar o desenho.

Requer somente Pillow. A centralização usa o bounding box real do canal alpha,
evitando que transparência assimétrica desloque visualmente o símbolo.
"""

from pathlib import Path
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "branding/generated/escala-ici-icon-foreground.png"
ANDROID_RES = ROOT / "composeApp/src/androidMain/res"
BACKGROUND = (47, 20, 92, 255)  # #2F145C, mantido da identidade oficial.


def centered_symbol(source: Image.Image, canvas_size: int, coverage: float) -> Image.Image:
    source = source.convert("RGBA")
    bounds = source.getchannel("A").getbbox()
    if bounds is None:
        raise ValueError("A fonte não contém pixels visíveis")

    symbol = source.crop(bounds)
    target = round(canvas_size * coverage)
    scale = min(target / symbol.width, target / symbol.height)
    size = (max(1, round(symbol.width * scale)), max(1, round(symbol.height * scale)))
    symbol = symbol.resize(size, Image.Resampling.LANCZOS)

    canvas = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    position = ((canvas_size - size[0]) // 2, (canvas_size - size[1]) // 2)
    canvas.alpha_composite(symbol, position)
    return canvas


def legacy_icon(source: Image.Image, size: int, *, round_icon: bool) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), BACKGROUND)
    canvas.alpha_composite(centered_symbol(source, size, 0.60))
    if round_icon:
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        canvas.putalpha(mask)
    return canvas


def save(image: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, optimize=True)


def main() -> None:
    source = Image.open(SOURCE).convert("RGBA")

    # Fonte canônica com margem uniforme; preserva integralmente a arte recortada.
    master = centered_symbol(source, 1024, 0.60)
    save(master, SOURCE)

    adaptive = centered_symbol(master, 432, 0.58)
    splash = centered_symbol(master, 432, 0.48)
    save(adaptive, ANDROID_RES / "drawable-xxxhdpi/ic_launcher_foreground.png")
    save(splash, ANDROID_RES / "drawable-xxxhdpi/ic_splash_icon.png")

    densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for density, size in densities.items():
        directory = ANDROID_RES / f"mipmap-{density}"
        save(legacy_icon(master, size, round_icon=False), directory / "ic_launcher.png")
        save(legacy_icon(master, size, round_icon=True), directory / "ic_launcher_round.png")


if __name__ == "__main__":
    main()
