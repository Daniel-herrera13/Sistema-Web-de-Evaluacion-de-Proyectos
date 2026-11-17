package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ReporteDto;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

@Service
public class ExportService {

    public byte[] excel(ReporteDto.Tabla tabla) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte");

            org.apache.poi.ss.usermodel.Font negrita = workbook.createFont();
            negrita.setBold(true);
            CellStyle estiloCabecera = workbook.createCellStyle();
            estiloCabecera.setFont(negrita);

            Row header = sheet.createRow(0);
            for (int i = 0; i < tabla.cabeceras().size(); i++) {
                var celda = header.createCell(i);
                celda.setCellValue(tabla.cabeceras().get(i));
                celda.setCellStyle(estiloCabecera);
            }
            for (int rowIndex = 0; rowIndex < tabla.filas().size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < tabla.filas().get(rowIndex).size(); columnIndex++) {
                    Object value = tabla.filas().get(rowIndex).get(columnIndex);
                    if (value instanceof Number number) {
                        row.createCell(columnIndex).setCellValue(number.doubleValue());
                    } else {
                        row.createCell(columnIndex).setCellValue(Objects.toString(value, ""));
                    }
                }
            }
            sheet.createFreezePane(0, 1);   // el encabezado queda fijo al desplazarse
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
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 28);   // horizontal
            PdfWriter.getInstance(document, output);
            document.open();

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
}