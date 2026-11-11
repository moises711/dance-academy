package academia.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Exporta las tablas de la sesión como un libro Open XML. */
final class ExcelExport {
    private static final String SHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String OFFICE_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/";
    private static final String PACKAGE_REL = "http://schemas.openxmlformats.org/package/2006/relationships";

    private record Sheet(String name, List<String> headers, List<? extends List<String>> rows) {}

    static void write(Path destination, List<PreviewData.Module> modules,
                      PreviewData.Section dashboard) throws IOException {
        var sheets = new ArrayList<Sheet>();
        var index = new ArrayList<List<String>>();
        sheets.add(new Sheet("Índice", List.of("Módulo", "Hoja", "Registros"), index));

        for (var module : modules) {
            var sections = module.sections().isEmpty() ? List.of(dashboard) : module.sections();
            for (var section : sections) {
                String name = module.name().equals(section.name()) ? module.name()
                    : module.name() + " - " + section.name();
                if (name.length() > 31) {
                    name = name.substring(0, 31);
                }
                sheets.add(new Sheet(name, section.columns(), section.rows()));
                index.add(List.of(module.name(), name, Integer.toString(section.rows().size())));
            }
        }

        var target = destination.toAbsolutePath();
        var temporary = Files.createTempFile(target.getParent(), "academia-", ".xlsx");
        try {
            try (var zip = new ZipOutputStream(Files.newOutputStream(temporary))) {
                writePackage(zip, sheets);
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writePackage(ZipOutputStream zip, List<Sheet> sheets) throws IOException {
        var types = new StringBuilder("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        var workbook = new StringBuilder("<workbook xmlns=\"" + SHEET_NS + "\" xmlns:r=\"" + OFFICE_REL.substring(0, OFFICE_REL.length() - 1) + "\"><sheets>");
        var relationships = new StringBuilder("<Relationships xmlns=\"" + PACKAGE_REL + "\">");
        for (int i = 0; i < sheets.size(); i++) {
            int id = i + 1;
            String sheetPath = "worksheets/sheet" + id + ".xml";
            types.append("<Override PartName=\"/xl/").append(sheetPath)
                .append("\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
            workbook.append("<sheet name=\"").append(xml(sheets.get(i).name()))
                .append("\" sheetId=\"").append(id).append("\" r:id=\"rId").append(id).append("\"/>");
            relationships.append(relationship(id, "worksheet", sheetPath));
            entry(zip, "xl/" + sheetPath, worksheet(sheets.get(i), i == 0));
        }
        entry(zip, "[Content_Types].xml", types.append("</Types>").toString());
        entry(zip, "_rels/.rels", "<Relationships xmlns=\"" + PACKAGE_REL + "\">"
            + relationship(1, "officeDocument", "xl/workbook.xml") + "</Relationships>");
        entry(zip, "xl/workbook.xml", workbook.append("</sheets></workbook>").toString());
        entry(zip, "xl/_rels/workbook.xml.rels", relationships.append("</Relationships>").toString());
    }

    private static String worksheet(Sheet sheet, boolean index) {
        var out = new StringBuilder("<worksheet xmlns=\"" + SHEET_NS + "\">");
        out.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        out.append("<cols><col min=\"1\" max=\"").append(sheet.headers().size())
            .append("\" width=\"25\" customWidth=\"1\"/></cols><sheetData>");
        row(out, 1, sheet.headers());
        for (int i = 0; i < sheet.rows().size(); i++) {
            row(out, i + 2, sheet.rows().get(i));
        }
        out.append("</sheetData><autoFilter ref=\"A1:").append((char) ('A' + sheet.headers().size() - 1))
            .append(sheet.rows().size() + 1).append("\"/>");
        if (index) {
            out.append("<hyperlinks>");
            for (int i = 0; i < sheet.rows().size(); i++) {
                out.append("<hyperlink ref=\"B").append(i + 2).append("\" location=\"&apos;")
                    .append(xml(sheet.rows().get(i).get(1))).append("&apos;!A1\"/>");
            }
            out.append("</hyperlinks>");
        }
        return out.append("</worksheet>").toString();
    }

    private static void row(StringBuilder out, int number, List<String> values) {
        out.append("<row r=\"").append(number).append("\">");
        for (int i = 0; i < values.size(); i++) {
            out.append("<c r=\"").append((char) ('A' + i)).append(number)
                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                .append(xml(values.get(i))).append("</t></is></c>");
        }
        out.append("</row>");
    }

    private static String relationship(int id, String type, String target) {
        return "<Relationship Id=\"rId" + id + "\" Type=\"" + OFFICE_REL + type
            + "\" Target=\"" + target + "\"/>";
    }

    private static String xml(String value) {
        return value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\uFFFE\\uFFFF]", "")
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
