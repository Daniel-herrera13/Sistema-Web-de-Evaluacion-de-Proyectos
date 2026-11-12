package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.ReporteDto;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

@Service
public class ExportService {

    public byte[] excel(ReporteDto.Tabla tabla) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte");
            Row header = sheet.createRow(0);
            for (int i = 0; i < tabla.cabeceras().size(); i++) {
                header.createCell(i).setCellValue(tabla.cabeceras().get(i));
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
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph(tabla.titulo()));

            PdfPTable table = new PdfPTable(tabla.cabeceras().size());
            for (String header : tabla.cabeceras()) {
                table.addCell(new Phrase(header));
            }
            for (var fila : tabla.filas()) {
                for (Object value : fila) {
                    table.addCell(new Phrase(Objects.toString(value, "")));
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
