"""Mod-wide language keys."""

from datagen import name

TIER_NAMES = {1: "Muscle and Timber", 2: "Steam and Copper", 3: "Electric Works", 4: "Frontier", 5: "Matter", 6: "Ascendant"}


def generate():
    for t, n in TIER_NAMES.items():
        name(f"itemGroup.autarkia.tier_{t}", f"Autarkia T{t}: {n}")
    name("key.category.autarkia.main", "Autarkia")
    name("key.autarkia.mode", "Cycle tool mode")
    name("key.autarkia.enchant", "Cycle enchant mode")
    name("key.autarkia.suit", "Toggle suit functions")
    name("autarkia.configuration.title", "Autarkia")
