"""M10 data completeness (12 section 2 M10): the empty item tag autarkia:lattice_tool_materials that the lattice tool
material names as its repair tag (07 section 1.1: lattice tools repair with none), written as a file so datapacks can fill it."""

from datagen import write_json


def generate():
    write_json("data/autarkia/tags/item/lattice_tool_materials.json", {"replace": False, "values": []})
