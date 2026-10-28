# Translating GregTech 6

A translation is plain JSON files. There is no code to write: a file put in the right place is picked up by the game when its language is selected
(after the usual build of the mod). Each language needs up to three files, one per mod namespace:

| Start from (English) | Save as | Put it in |
|---|---|---|
| `gregtech6.en.json` | `<code>.json` | `src/main/resources/assets/gregtech6/lang/` |
| `gregapi.en.json` | `<code>.json` | `src/main/resources/assets/gregapi/lang/` |
| `gt6u.en.json` | `<code>.json` | `src/gt6u/resources/assets/gt6u/lang/` |

`<code>` is Minecraft's language code in lower case: `zh_cn`, `ja_jp`, `ko_kr`, `de_de`. The first file is the mod itself (about 100 000 texts, most of
them names of items made from a material and a shape). The second has a handful of texts (the names of the mod's resource packs). The third is for the GT6U addon, a separate jar; translate it if you
play with the addon. A language file can be sent on its own, without the other two.

## How

1. Copy a template to `<code>.json`. Change the values, never the keys.
2. Run the checker (Python 3, nothing to install):
   `python check_lang.py <code>.json --template gregtech6.en.json`
3. A partial translation is fine: a text that is not in your file is shown in English. `--missing todo.json` writes a file with only what is left.
4. Select the language in the game and look at it. The checker proves the file is sound, not that it reads well.

## What must stay inside a text

- **Colour codes** `§c`, `§7` ... keep the same codes.
- **Placeholders** `%s`, `%d` ... keep them.
- **`¶`** is a line break in a book page; keep as many as the English has.
- **Spaces at the start and the end.** Texts are joined with other texts: `This Mold produces ` ends with a space on purpose.
- **A book page must stay shorter than 256 characters**, or the game drops that page (`written.book.<name>.page.<n>`).

## Keys that need a word

- `gt.tt.<text>.<hash>`: the name is made from the English text. If the English changes in a later version, the key changes, and the old translation
  stops applying (the English shows) until the template is built again.
- `gt.material.<name>` is the name of a material when it is listed; `oredict.<shape><Material>` is the name of an item. They are ordinary texts: write
  them out or generate them with your own script. Equal English should get equal translations.
- **GT6U only.** The addon changes the text of a few GT6 keys (descriptions of some machines, two food tooltips) and the pages of the Book of Alloys
  and the Book of Elements. The translation of such a key goes in the GT6U file as `gt6u.rewrite.<key>`: the game uses it in place of GT6's own text.
  Do not copy a key of `gregtech6.en.json` into the GT6U file: the game merges the files of different mods in no fixed order, so it would never apply.
  `python check_lang.py <code>.json --template gt6u.en.json --core-template gregtech6.en.json` reports it.

## Sending it

A pull request with the file(s) in their places, or the file(s) sent to the maintainers. Say where the translation comes from; do not include text from
a source that does not allow it. A contribution is licensed as the rest of the project (LGPL v3 or later, see `CONTRIBUTING.md`).
The `main` and `1.20.1` branches keep their own copies of a language file and their own templates; the keys are nearly the same.

## About the templates

`TEMPLATE.txt` says which commit a template was built from and how many keys it has. They are generated, not edited:
`workspace/tools/lang-ru/build_lang_templates.py` in the maintainers' environment. `python check_lang.py --selftest` checks the checker on files that are
wrong in known ways.
