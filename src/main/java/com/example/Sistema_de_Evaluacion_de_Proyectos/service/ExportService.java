package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ReporteDto;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ExportService {

    public byte[] excel(ReporteDto.Tabla tabla) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte");

            int pictureIndex = workbook.addPicture(logoBytes(), Workbook.PICTURE_TYPE_PNG);
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(0);
            anchor.setRow1(0);
            anchor.setCol2(6);
            anchor.setRow2(4);
            drawing.createPicture(anchor, pictureIndex);

            Row titulo = sheet.createRow(5);
            titulo.setHeightInPoints(24);
            titulo.createCell(0).setCellValue(tabla.titulo());
            org.apache.poi.ss.usermodel.Font fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            fuenteTitulo.setColor(IndexedColors.DARK_RED.getIndex());
            CellStyle estiloTitulo = workbook.createCellStyle();
            estiloTitulo.setFont(fuenteTitulo);
            titulo.getCell(0).setCellStyle(estiloTitulo);
            if (!tabla.cabeceras().isEmpty()) {
                sheet.addMergedRegion(new CellRangeAddress(5, 5, 0, tabla.cabeceras().size() - 1));
            }
            Row fecha = sheet.createRow(6);
            fecha.createCell(0).setCellValue("ITCA-FEPADE · Reporte académico · "
                    + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

            org.apache.poi.ss.usermodel.Font negrita = workbook.createFont();
            negrita.setBold(true);
            CellStyle estiloCabecera = workbook.createCellStyle();
            estiloCabecera.setFont(negrita);

            Row header = sheet.createRow(7);
            for (int i = 0; i < tabla.cabeceras().size(); i++) {
                var celda = header.createCell(i);
                celda.setCellValue(tabla.cabeceras().get(i));
                celda.setCellStyle(estiloCabecera);
            }
            for (int rowIndex = 0; rowIndex < tabla.filas().size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 8);
                for (int columnIndex = 0; columnIndex < tabla.filas().get(rowIndex).size(); columnIndex++) {
                    Object value = tabla.filas().get(rowIndex).get(columnIndex);
                    if (value instanceof Number number) {
                        row.createCell(columnIndex).setCellValue(number.doubleValue());
                    } else {
                        row.createCell(columnIndex).setCellValue(Objects.toString(value, ""));
                    }
                }
            }
            sheet.createFreezePane(0, 8);   // el encabezado queda fijo al desplazarse
            for (int i = 0; i < tabla.cabeceras().size(); i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }

    public byte[] pdf(ReporteDto.Tabla tabla) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            boolean detalleProyecto = tabla.titulo() != null && tabla.titulo().startsWith("Proyecto:");
            Document document = new Document(detalleProyecto ? PageSize.LETTER : PageSize.A4.rotate(),
                    36, 36, detalleProyecto ? 34 : 28, 34);
            PdfWriter.getInstance(document, output);
            document.open();

            if (detalleProyecto) {
                agregarPdfIndividual(document, tabla);
                document.close();
                return output.toByteArray();
            }

            Image logo = imagenInstitucional();
            logo.scaleToFit(155, 38);
            logo.setAlignment(Element.ALIGN_LEFT);
            document.add(logo);
            Paragraph titulo = new Paragraph(tabla.titulo(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            titulo.setSpacingAfter(10f);
            document.add(titulo);

            Font fuenteCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, 8);

            PdfPTable table = new PdfPTable(tabla.cabeceras().size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1);   // el encabezado se repite en cada página
            for (String header : tabla.cabeceras()) {
                PdfPCell celda = new PdfPCell(new Phrase(header, fuenteCabecera));
                celda.setBackgroundColor(Color.LIGHT_GRAY);
                celda.setPadding(4f);
                table.addCell(celda);
            }
            for (var fila : tabla.filas()) {
                for (Object value : fila) {
                    PdfPCell celda = new PdfPCell(new Phrase(Objects.toString(value, ""), fuenteCelda));
                    celda.setPadding(3f);
                    table.addCell(celda);
                }
            }
            document.add(table);
            document.close();
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el archivo PDF", e);
        }
    }

    /** Reporte individual con el formato institucional del ejemplo de defensa. */
    private void agregarPdfIndividual(Document document, ReporteDto.Tabla tabla) throws Exception {
        Color institucional = new Color(179, 27, 27);
        Color texto = new Color(26, 34, 47);
        Color etiqueta = new Color(74, 85, 104);
        Color fondo = new Color(248, 249, 251);
        Color borde = new Color(204, 213, 224);
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(198, 138, 0));
        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        Font etiquetaFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, etiqueta);
        Font valorFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, texto);
        Font seccionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font th = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, texto);
        Font td = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, texto);

        PdfPTable encabezado = new PdfPTable(new float[]{3.2f, 1f});
        encabezado.setWidthPercentage(100);
        encabezado.setSpacingAfter(7f);
        PdfPCell marcaCell = new PdfPCell();
        Image logo = imagenInstitucional();
        logo.scaleToFit(160, 42);
        marcaCell.addElement(logo);
        marcaCell.setBackgroundColor(Color.WHITE);
        marcaCell.setPadding(5);
        marcaCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        marcaCell.setBorderColorBottom(institucional);
        marcaCell.setBorderWidthBottom(1.5f);
        encabezado.addCell(marcaCell);
        PdfPCell fecha = celda(new Phrase("EMISIÓN\n" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), etiquetaFont), Color.WHITE, 5, Element.ALIGN_RIGHT);
        fecha.setBorderColorBottom(institucional);
        fecha.setBorderWidthBottom(1.5f);
        encabezado.addCell(fecha);
        document.add(encabezado);

        String tituloProyecto = parteTitulo(tabla.titulo(), "Proyecto:", "|");
        String materia = parteTitulo(tabla.titulo(), "Materia:", "|");
        String seccion = parteTitulo(tabla.titulo(), "Sección:", "|");
        String grupo = parteTitulo(tabla.titulo(), "Grupo:", "|");
        String ciclo = parteTitulo(tabla.titulo(), "Ciclo:", "|");
        String empresa = parteTitulo(tabla.titulo(), "Empresa:", "|");
        String estadoProyecto = parteTitulo(tabla.titulo(), "Estado:", "|");
        String publicacion = parteTitulo(tabla.titulo(), "Publicación:", "|");
        String nota = parteTitulo(tabla.titulo(), "Nota docentes/jurado:", "|");
        PdfPTable banda = new PdfPTable(1);
        banda.setWidthPercentage(100);
        PdfPCell bandaCell = celda(new Phrase("REPORTE INDIVIDUAL DEL PROYECTO", titulo), institucional, 8, Element.ALIGN_CENTER);
        bandaCell.setBorder(Rectangle.NO_BORDER);
        banda.addCell(bandaCell);
        document.add(banda);

        Paragraph contexto = new Paragraph("Escuela de Computación e Ingeniería | Reporte académico", subtitulo);
        contexto.setAlignment(Element.ALIGN_CENTER);
        contexto.setSpacingBefore(3f);
        contexto.setSpacingAfter(10f);
        document.add(contexto);

        PdfPTable identificacion = new PdfPTable(new float[]{1f, 2.3f, 1f, 2.3f});
        identificacion.setWidthPercentage(100);
        identificacion.setSpacingAfter(11f);
        agregarCampo(identificacion, "PROYECTO", tituloProyecto, fondo, borde, etiquetaFont, valorFont);
        agregarCampo(identificacion, "MATERIA", materia, fondo, borde, etiquetaFont, valorFont);
        agregarCampo(identificacion, "SECCIÓN", seccion, fondo, borde, etiquetaFont, valorFont);
        agregarCampo(identificacion, "GRUPO", grupo, fondo, borde, etiquetaFont, valorFont);
        agregarCampo(identificacion, "CICLO", ciclo, fondo, borde, etiquetaFont, valorFont);
        agregarCampo(identificacion, "EMPRESA", empresa, fondo, borde, etiquetaFont, valorFont);
        agregarCampo(identificacion, "ESTADO", estadoProyecto, fondo, borde, etiquetaFont, valorFont);
        agregarCampo(identificacion, "PUBLICACIÓN", publicacion, fondo, borde, etiquetaFont, valorFont);
        if (!nota.isBlank()) {
            agregarCampo(identificacion, "NOTA DOCENTES/JURADO", nota, fondo, borde, etiquetaFont, valorFont);
            agregarCampo(identificacion, "", "", Color.WHITE, Color.WHITE, etiquetaFont, valorFont);
        }
        document.add(identificacion);

        List<List<Object>> evaluadores = new ArrayList<>();
        List<List<Object>> criterios = new ArrayList<>();
        List<List<Object>> alumnos = new ArrayList<>();
        for (List<Object> fila : tabla.filas()) {
            if (fila.isEmpty()) continue;
            String concepto = Objects.toString(fila.get(0), "");
            if (concepto.startsWith("Evaluador:")) evaluadores.add(fila);
            else if (concepto.startsWith("Criterio:")) criterios.add(fila);
            else if (concepto.startsWith("Alumno:")) alumnos.add(fila);
        }
        agregarSeccion(document, "EVALUADORES", "Evaluador:", evaluadores, institucional, fondo, borde, seccionFont, th, td);
        agregarSeccion(document, "CRITERIOS DE EVALUACIÓN", "Criterio:", criterios, institucional, fondo, borde, seccionFont, th, td);
        agregarSeccion(document, "RESULTADOS POR ALUMNO", "Alumno:", alumnos, institucional, fondo, borde, seccionFont, th, td);

        Paragraph pie = new Paragraph("Documento emitido con fines docentes y de control académico interno.",
                FontFactory.getFont(FontFactory.HELVETICA, 6.5f, new Color(113, 126, 145)));
        pie.setSpacingBefore(12f);
        pie.setAlignment(Element.ALIGN_CENTER);
        document.add(pie);
    }

    private void agregarSeccion(Document document, String titulo, String prefijo, List<List<Object>> filas,
                                Color institucional, Color fondo, Color borde, Font fuenteSeccion,
                                Font fuenteCabecera, Font fuenteCelda) throws Exception {
        if (filas.isEmpty()) return;
        PdfPTable cabecera = new PdfPTable(1);
        cabecera.setWidthPercentage(100);
        cabecera.setKeepTogether(true);
        cabecera.setSpacingBefore(5f);
        PdfPCell banda = celda(new Phrase(titulo, fuenteSeccion), institucional, 6, Element.ALIGN_LEFT);
        banda.setBorder(Rectangle.NO_BORDER);
        cabecera.addCell(banda);
        document.add(cabecera);

        PdfPTable tabla = new PdfPTable(new float[]{2.2f, 3.2f, 1.5f, 1f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        tabla.setSpacingAfter(4f);
        for (String texto : List.of("PERSONA / CRITERIO", "DETALLE", "ESTADO", "NOTA")) {
            PdfPCell c = celda(new Phrase(texto, fuenteCabecera), new Color(237, 242, 247), 5, Element.ALIGN_LEFT);
            c.setBorderColor(borde);
            tabla.addCell(c);
        }
        for (List<Object> fila : filas) {
            String nombre = Objects.toString(fila.size() > 0 ? fila.get(0) : "", "").replace(prefijo, "").trim();
            Object detalle = fila.size() > 1 ? fila.get(1) : "";
            Object estado = fila.size() > 2 ? fila.get(2) : "";
            Object nota = fila.size() > 3 ? fila.get(3) : "";
            for (Object valor : List.of(nombre, Objects.toString(detalle, ""), Objects.toString(estado, ""), Objects.toString(nota, ""))) {
                PdfPCell c = celda(new Phrase(Objects.toString(valor, ""), fuenteCelda), Color.WHITE, 5, Element.ALIGN_LEFT);
                c.setBorderColor(borde);
                tabla.addCell(c);
            }
        }
        document.add(tabla);
    }

    private void agregarCampo(PdfPTable tabla, String etiqueta, String valor, Color fondo, Color borde,
                              Font fuenteEtiqueta, Font fuenteValor) {
        PdfPCell e = celda(new Phrase(etiqueta, fuenteEtiqueta), fondo, 6, Element.ALIGN_LEFT);
        PdfPCell v = celda(new Phrase(valor, fuenteValor), Color.WHITE, 6, Element.ALIGN_LEFT);
        e.setBorderColor(borde);
        v.setBorderColor(borde);
        tabla.addCell(e);
        tabla.addCell(v);
    }

    private PdfPCell celda(Phrase contenido, Color fondo, float padding, int alineacion) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBackgroundColor(fondo);
        celda.setPadding(padding);
        celda.setHorizontalAlignment(alineacion);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    private byte[] logoBytes() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/branding/itca-fepade-logo.png")) {
            if (input == null) throw new IOException("No se encontró el logo institucional en /branding/itca-fepade-logo.png");
            return input.readAllBytes();
        }
    }

    private Image imagenInstitucional() throws Exception {
        return Image.getInstance(logoBytes());
    }

    private String parteTitulo(String titulo, String inicio, String fin) {
        int desde = titulo.indexOf(inicio);
        if (desde < 0) return "";
        desde += inicio.length();
        int hasta = fin == null ? -1 : titulo.indexOf(fin, desde);
        return (hasta < 0 ? titulo.substring(desde) : titulo.substring(desde, hasta)).trim();
    }
}
