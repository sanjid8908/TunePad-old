package com.megh.notepad;

import android.app.Activity;
import android.os.Build;
import android.widget.Toast;
import java.io.File;
import java.util.List;

public class ExportHelper {

    // ==========================================
    // 🌟 আল্টিমেট নেটিভ PDF জেনারেটর (Crash Fixed + TOC Page Numbers) 🌟
    // ==========================================
    public static void exportToPdf(final Activity activity, final String documentTitle, final String authorName, final String dedicationText, final List<String[]> contents, final android.graphics.Typeface ignoredTypeface, final File projectDir) {
        Toast.makeText(activity, "প্রফেশনাল বই তৈরি হচ্ছে, পেজ নাম্বার ক্যালকুলেট করা হচ্ছে...", Toast.LENGTH_LONG).show();

        new Thread(() -> {
            try {
                android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
                int pageWidth = 595; 
                int pageHeight = 842; 
                int marginX = 65; 
                int marginY = 80; 
                int marginBottom = 70; 

                android.graphics.Typeface pdfTypeface;
                try {
                    pdfTypeface = android.graphics.Typeface.createFromAsset(activity.getAssets(), "fonts/solaimanlipi.ttf");
                } catch (Exception e) {
                    pdfTypeface = android.graphics.Typeface.DEFAULT;
                }

                // ==========================================
                // 👻 STEP 1: GHOST RENDERING (পেজ গোনার জন্য অদৃশ্য পাস)
                // ==========================================
                int[] chapterStartPages = new int[contents.size()];
                int simPageNum = 1;

                boolean hasCover = (projectDir != null && new File(projectDir, "cover.jpg").exists());
                if (hasCover) simPageNum++; // কভার পেজ
                simPageNum++; // টাইটেল পেজ
                
                // উৎসর্গপত্র ফাঁকা না থাকলেই কেবল পেজ গুনবে
                if (dedicationText != null && !dedicationText.trim().isEmpty()) {
                    simPageNum++; 
                }

                // সূচিপত্র কত পেজ নেবে সেটা গোনা
                if (contents.size() > 0) {
                    int tocY = marginY + 80;
                    for (int i = 0; i < contents.size(); i++) {
                        String chapterTitle = contents.get(i)[0];
                        if (chapterTitle != null && !chapterTitle.isEmpty()) {
                            if (tocY > pageHeight - marginBottom) {
                                simPageNum++;
                                tocY = marginY + 40;
                            }
                            tocY += 35;
                        }
                    }
                    simPageNum++; // সূচিপত্র শেষ
                }

                android.text.TextPaint simPaint = new android.text.TextPaint();
                simPaint.setTypeface(pdfTypeface);
                simPaint.setTextSize(14.0f);

                for (int i = 0; i < contents.size(); i++) {
                    chapterStartPages[i] = simPageNum; // 🌟 পর্বের শুরুর পেজ নাম্বার সেভ

                    String rawContent = contents.get(i)[1];
                    if (rawContent == null) rawContent = "";
                    int currentY = marginY;
                    String chapterTitle = contents.get(i)[0];

                    if (chapterTitle != null && !chapterTitle.isEmpty()) {
                        if (currentY + 100 > pageHeight - marginBottom) { simPageNum++; currentY = marginY; }
                        currentY += 80; 
                    }

                    rawContent = rawContent.replace("<br>", "\n").replaceAll("<[^>]*>", "").replaceAll("\n{3,}", "\n\n").replaceAll("\n", "\n      ");
                    rawContent = "      " + rawContent;

                    android.text.StaticLayout layout;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        layout = android.text.StaticLayout.Builder.obtain(rawContent, 0, rawContent.length(), simPaint, pageWidth - (2 * marginX))
                                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0.0f, 1.05f).build();
                    } else {
                        layout = new android.text.StaticLayout(rawContent, simPaint, pageWidth - (2 * marginX), android.text.Layout.Alignment.ALIGN_NORMAL, 1.05f, 0.0f, false);
                    }

                    int totalHeight = layout.getHeight();
                    int currentLayoutY = 0; 
                    while (currentLayoutY < totalHeight) {
                        int availableHeight = pageHeight - marginBottom - currentY; 
                        if (availableHeight < 40) { 
                            simPageNum++;
                            currentY = marginY;
                            availableHeight = pageHeight - marginBottom - currentY;
                        }
                        int startLine = layout.getLineForVertical(currentLayoutY);
                        int endLine = layout.getLineForVertical(currentLayoutY + availableHeight);
                        if (layout.getLineBottom(endLine) > currentLayoutY + availableHeight && endLine > startLine) { endLine--; }
                        
                        int drawHeight = layout.getLineBottom(endLine) - currentLayoutY;
                        
                        // 🌟 ইনফিনিট লুপ ফিক্স (Failsafe) 🌟
                        if (drawHeight <= 0) { 
                            drawHeight = availableHeight; 
                            if(drawHeight <= 0) drawHeight = 20; 
                        }
                        
                        currentLayoutY += drawHeight;
                        currentY += drawHeight;
                    }
                    simPageNum++; 
                }

                // ==========================================
                // 🎨 STEP 2: ACTUAL RENDERING (আসল পিডিএফ তৈরি)
                // ==========================================
                int pageNum = 1;

              // ১. কভার পেজ
                if (hasCover) {
                    android.graphics.pdf.PdfDocument.PageInfo coverInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create();
                    android.graphics.pdf.PdfDocument.Page coverPage = document.startPage(coverInfo);
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(new File(projectDir, "cover.jpg").getAbsolutePath());
                    if (bitmap != null) {
                        float pdfRatio = (float) pageWidth / pageHeight;
                        float bmpRatio = (float) bitmap.getWidth() / bitmap.getHeight();
                        int srcX = 0, srcY = 0, srcW = bitmap.getWidth(), srcH = bitmap.getHeight();
                        if (bmpRatio > pdfRatio) { srcW = (int) (srcH * pdfRatio); srcX = (bitmap.getWidth() - srcW) / 2; } 
                        else { srcH = (int) (srcW / pdfRatio); srcY = (bitmap.getHeight() - srcH) / 2; }
                        
                        // 🌟 ফিক্সড: document.startPage() এর বদলে সরাসরি coverPage.getCanvas() ব্যবহার করা হলো 🌟
                        coverPage.getCanvas().drawBitmap(bitmap, new android.graphics.Rect(srcX, srcY, srcX + srcW, srcY + srcH), new android.graphics.Rect(0, 0, pageWidth, pageHeight), null);
                    }
                    document.finishPage(coverPage);
                }

                // ২. টাইটেল ও লেখিকার নাম
                android.graphics.pdf.PdfDocument.PageInfo titlePageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create();
                android.graphics.pdf.PdfDocument.Page titlePage = document.startPage(titlePageInfo);
                android.graphics.Canvas titleCanvas = titlePage.getCanvas();
                android.text.TextPaint bookTitlePaint = new android.text.TextPaint();
                bookTitlePaint.setTypeface(android.graphics.Typeface.create(pdfTypeface, android.graphics.Typeface.BOLD));
                bookTitlePaint.setTextSize(36f);
                bookTitlePaint.setColor(android.graphics.Color.BLACK);
                bookTitlePaint.setTextAlign(android.graphics.Paint.Align.CENTER);
                titleCanvas.drawText(documentTitle, pageWidth / 2f, pageHeight / 2.5f, bookTitlePaint);

                if (authorName != null && !authorName.trim().isEmpty()) {
                    android.text.TextPaint authorPaint = new android.text.TextPaint();
                    authorPaint.setTypeface(pdfTypeface);
                    authorPaint.setTextSize(20f);
                    authorPaint.setColor(android.graphics.Color.DKGRAY);
                    authorPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
                    titleCanvas.drawText("লেখিকা: " + authorName, pageWidth / 2f, (pageHeight / 2.5f) + 60, authorPaint);
                }
                document.finishPage(titlePage);

                // ৩. উৎসর্গপত্র (যদি ফাঁকা না থাকে)
                if (dedicationText != null && !dedicationText.trim().isEmpty()) {
                    android.graphics.pdf.PdfDocument.PageInfo dedPageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create();
                    android.graphics.pdf.PdfDocument.Page dedPage = document.startPage(dedPageInfo);
                    android.graphics.Canvas dedCanvas = dedPage.getCanvas();

                    android.text.TextPaint dedTitlePaint = new android.text.TextPaint();
                    dedTitlePaint.setTypeface(android.graphics.Typeface.create(pdfTypeface, android.graphics.Typeface.BOLD));
                    dedTitlePaint.setTextSize(24f);
                    dedTitlePaint.setTextAlign(android.graphics.Paint.Align.CENTER);
                    dedCanvas.drawText("উৎসর্গ", pageWidth / 2f, pageHeight / 3f, dedTitlePaint);

                    android.text.TextPaint dedBodyPaint = new android.text.TextPaint();
                    dedBodyPaint.setTypeface(pdfTypeface); 
                    dedBodyPaint.setTextSize(16f);
                    dedBodyPaint.setColor(android.graphics.Color.DKGRAY);
                    
                    String cleanDedication = dedicationText.replace("<br>", "\n").replaceAll("<[^>]*>", "");
                    android.text.StaticLayout dedLayout;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        dedLayout = android.text.StaticLayout.Builder.obtain(cleanDedication, 0, cleanDedication.length(), dedBodyPaint, pageWidth - 160)
                                .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setLineSpacing(0.0f, 1.3f).build();
                    } else {
                        dedLayout = new android.text.StaticLayout(cleanDedication, dedBodyPaint, pageWidth - 160, android.text.Layout.Alignment.ALIGN_CENTER, 1.3f, 0.0f, false);
                    }
                    dedCanvas.save();
                    dedCanvas.translate(80, (pageHeight / 3f) + 40);
                    dedLayout.draw(dedCanvas);
                    dedCanvas.restore();
                    document.finishPage(dedPage);
                }

                // ৪. সূচিপত্র (ডায়নামিক পেজ নাম্বার এবং ডট ডট স্টাইলসহ)
                if (contents.size() > 0) {
                    android.graphics.pdf.PdfDocument.PageInfo tocPageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create();
                    android.graphics.pdf.PdfDocument.Page tocPage = document.startPage(tocPageInfo);
                    android.graphics.Canvas tocCanvas = tocPage.getCanvas();

                    android.text.TextPaint tocTitlePaint = new android.text.TextPaint();
                    tocTitlePaint.setTypeface(android.graphics.Typeface.create(pdfTypeface, android.graphics.Typeface.BOLD));
                    tocTitlePaint.setTextSize(28f);
                    tocTitlePaint.setTextAlign(android.graphics.Paint.Align.CENTER);
                    tocCanvas.drawText("সূচিপত্র", pageWidth / 2f, marginY + 20, tocTitlePaint);

                    android.text.TextPaint tocItemPaint = new android.text.TextPaint();
                    tocItemPaint.setTypeface(pdfTypeface);
                    tocItemPaint.setTextSize(16f);
                    tocItemPaint.setColor(android.graphics.Color.DKGRAY);

                    int tocY = marginY + 80;
                    
                    for (int i = 0; i < contents.size(); i++) {
                        String chapterTitle = contents.get(i)[0];
                        if (chapterTitle != null && !chapterTitle.isEmpty()) {
                            if (tocY > pageHeight - marginBottom) {
                                document.finishPage(tocPage);
                                tocPageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum++).create();
                                tocPage = document.startPage(tocPageInfo);
                                tocCanvas = tocPage.getCanvas();
                                tocY = marginY + 40;
                            }
                            
                            // পর্বের নাম (বামে)
                            String chapterText = (i+1) + ".   " + chapterTitle;
                            tocItemPaint.setTextAlign(android.graphics.Paint.Align.LEFT);
                            tocCanvas.drawText(chapterText, marginX + 40, tocY, tocItemPaint);
                            
                            // পেজ নাম্বার (ডানে)
                            String pageStr = String.valueOf(chapterStartPages[i]);
                            tocItemPaint.setTextAlign(android.graphics.Paint.Align.RIGHT);
                            tocCanvas.drawText(pageStr, pageWidth - marginX - 40, tocY, tocItemPaint);
                            
                            // মাঝখানের ডট ডট
                            tocItemPaint.setTextAlign(android.graphics.Paint.Align.LEFT);
                            float textW = tocItemPaint.measureText(chapterText);
                            float numW = tocItemPaint.measureText(pageStr);
                            float dotSpace = (pageWidth - marginX - 40 - numW) - (marginX + 40 + textW) - 15;
                            
                            if (dotSpace > 0) {
                                float dotW = tocItemPaint.measureText(".");
                                int dotsCount = (int)(dotSpace / dotW);
                                StringBuilder dots = new StringBuilder();
                                for(int d=0; d<dotsCount; d++) dots.append(".");
                                tocCanvas.drawText(dots.toString(), marginX + 40 + textW + 8, tocY, tocItemPaint);
                            }
                            tocY += 35; 
                        }
                    }
                    document.finishPage(tocPage);
                }

                // ৫. মূল কন্টেন্ট রেন্ডারিং
                android.text.TextPaint contentPaint = new android.text.TextPaint();
                contentPaint.setTypeface(pdfTypeface);
                contentPaint.setTextSize(14.0f);
                contentPaint.setColor(android.graphics.Color.BLACK);

                android.text.TextPaint chapterPaint = new android.text.TextPaint();
                chapterPaint.setTypeface(android.graphics.Typeface.create(pdfTypeface, android.graphics.Typeface.BOLD));
                chapterPaint.setTextSize(24f);
                chapterPaint.setTextAlign(android.graphics.Paint.Align.CENTER);

                android.text.TextPaint pageNumPaint = new android.text.TextPaint();
                pageNumPaint.setTypeface(pdfTypeface);
                pageNumPaint.setTextSize(11f);
                pageNumPaint.setColor(android.graphics.Color.GRAY);
                pageNumPaint.setTextAlign(android.graphics.Paint.Align.CENTER);

                android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
                android.graphics.Canvas canvas = page.getCanvas();
                int currentY = marginY;

                for (int i = 0; i < contents.size(); i++) {
                    String[] section = contents.get(i);
                    String chapterTitle = section[0];
                    String rawContent = section[1];
                    if (rawContent == null) rawContent = "";

                    if (i > 0) {
                        canvas.drawText("- " + pageNum + " -", pageWidth / 2f, pageHeight - 40, pageNumPaint);
                        document.finishPage(page);
                        pageNum++;
                        pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        currentY = marginY;
                    }

                    if (chapterTitle != null && !chapterTitle.isEmpty()) {
                        if (currentY + 100 > pageHeight - marginBottom) {
                            canvas.drawText("- " + pageNum + " -", pageWidth / 2f, pageHeight - 40, pageNumPaint);
                            document.finishPage(page);
                            pageNum++;
                            pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                            page = document.startPage(pageInfo);
                            canvas = page.getCanvas();
                            currentY = marginY;
                        }
                        currentY += 20; 
                        canvas.drawText(chapterTitle, pageWidth / 2f, currentY, chapterPaint);
                        currentY += 60; 
                    }

                    rawContent = rawContent.replace("<br>", "\n").replaceAll("<[^>]*>", "").replaceAll("\n{3,}", "\n\n").replaceAll("\n", "\n      ");
                    rawContent = "      " + rawContent;

                    android.text.StaticLayout layout;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        layout = android.text.StaticLayout.Builder.obtain(rawContent, 0, rawContent.length(), contentPaint, pageWidth - (2 * marginX))
                                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0.0f, 1.05f).setJustificationMode(android.text.Layout.JUSTIFICATION_MODE_INTER_WORD).build();
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        layout = android.text.StaticLayout.Builder.obtain(rawContent, 0, rawContent.length(), contentPaint, pageWidth - (2 * marginX))
                                .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0.0f, 1.05f).build();
                    } else {
                        layout = new android.text.StaticLayout(rawContent, contentPaint, pageWidth - (2 * marginX), android.text.Layout.Alignment.ALIGN_NORMAL, 1.05f, 0.0f, false);
                    }

                    int totalHeight = layout.getHeight();
                    int currentLayoutY = 0; 

                    while (currentLayoutY < totalHeight) {
                        int availableHeight = pageHeight - marginBottom - currentY; 
                        if (availableHeight < 40) { 
                            canvas.drawText("- " + pageNum + " -", pageWidth / 2f, pageHeight - 40, pageNumPaint);
                            document.finishPage(page);
                            pageNum++;
                            pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create();
                            page = document.startPage(pageInfo);
                            canvas = page.getCanvas();
                            currentY = marginY;
                            availableHeight = pageHeight - marginBottom - currentY;
                        }
                        int startLine = layout.getLineForVertical(currentLayoutY);
                        int endLine = layout.getLineForVertical(currentLayoutY + availableHeight);
                        if (layout.getLineBottom(endLine) > currentLayoutY + availableHeight && endLine > startLine) { endLine--; }
                        
                        int drawHeight = layout.getLineBottom(endLine) - currentLayoutY;
                        
                        // 🌟 ইনফিনিট লুপ ফিক্স (Failsafe) 🌟
                        if (drawHeight <= 0) { 
                            drawHeight = availableHeight; 
                            if(drawHeight <= 0) drawHeight = 20; 
                        }

                        canvas.save();
                        canvas.translate(marginX, currentY - currentLayoutY);
                        canvas.clipRect(0, currentLayoutY, pageWidth, currentLayoutY + drawHeight);
                        layout.draw(canvas);
                        canvas.restore();
                        
                        android.graphics.Paint maskPaint = new android.graphics.Paint();
                        maskPaint.setColor(android.graphics.Color.WHITE);
                        maskPaint.setStyle(android.graphics.Paint.Style.FILL);
                        canvas.drawRect(0, currentY + drawHeight, pageWidth, pageHeight, maskPaint);

                        currentLayoutY += drawHeight;
                        currentY += drawHeight;
                    }
                    currentY += 40; 
                }
                
                canvas.drawText("- " + pageNum + " -", pageWidth / 2f, pageHeight - 40, pageNumPaint);
                document.finishPage(page);

                File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                File tunePadDir = new File(downloadsDir, "TunePad");
                if (!tunePadDir.exists()) tunePadDir.mkdirs();
                File pdfFile = new File(tunePadDir, documentTitle + " - PDF.pdf");

                document.writeTo(new java.io.FileOutputStream(pdfFile));
                document.close();

                activity.runOnUiThread(() -> Toast.makeText(activity, "বইটি Downloads/TunePad ফোল্ডারে PDF হিসেবে সেভ হয়েছে! 🎉", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> Toast.makeText(activity, "PDF তৈরি করতে সমস্যা হয়েছে! " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }


    // ==========================================
    // 🌟 পিওর অরিজিনাল DOCX জেনারেটর (TOC Added + Dedication Logic) 🌟
    // ==========================================
    public static void exportToRealDocx(final Activity activity, final String documentTitle, final String authorName, final String dedicationText, final List<String[]> contents, final File projectDir) {
        Toast.makeText(activity, "আসল DOCX ফাইল তৈরি হচ্ছে, একটু অপেক্ষা করুন...", Toast.LENGTH_LONG).show();

        new Thread(() -> {
            try {
                File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                File tunePadDir = new File(downloadsDir, "TunePad");
                if (!tunePadDir.exists()) tunePadDir.mkdirs();
                
                File docxFile = new File(tunePadDir, documentTitle + " - TunePad.docx");

                java.io.FileOutputStream fos = new java.io.FileOutputStream(docxFile);
                java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(fos);

                zos.putNextEntry(new java.util.zip.ZipEntry("[Content_Types].xml"));
                String contentTypes = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">\n" +
                        "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>\n" +
                        "<Default Extension=\"xml\" ContentType=\"application/xml\"/>\n" +
                        "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>\n" +
                        "</Types>";
                zos.write(contentTypes.getBytes("UTF-8"));
                zos.closeEntry();

                zos.putNextEntry(new java.util.zip.ZipEntry("_rels/.rels"));
                String rels = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">\n" +
                        "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>\n" +
                        "</Relationships>";
                zos.write(rels.getBytes("UTF-8"));
                zos.closeEntry();

                zos.putNextEntry(new java.util.zip.ZipEntry("word/document.xml"));
                StringBuilder docXml = new StringBuilder();
                docXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
                docXml.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n");
                docXml.append("<w:body>\n");

                // ১. টাইটেল
                docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"56\"/><w:szCs w:val=\"56\"/></w:rPr><w:t>");
                docXml.append(escapeXml(documentTitle));
                docXml.append("</w:t></w:r></w:p>\n");

                // ২. লেখিকার নাম
                if(authorName != null && !authorName.trim().isEmpty()) {
                    docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:before=\"400\"/></w:pPr><w:r><w:rPr><w:sz w:val=\"36\"/><w:szCs w:val=\"36\"/></w:rPr><w:t>");
                    docXml.append(escapeXml("লেখিকা: " + authorName));
                    docXml.append("</w:t></w:r></w:p>\n");
                }
                docXml.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n");

                // ৩. উৎসর্গপত্র (যদি ফাঁকা না থাকে)
                if(dedicationText != null && !dedicationText.trim().isEmpty()) {
                    docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:before=\"1000\" w:after=\"400\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"40\"/><w:szCs w:val=\"40\"/></w:rPr><w:t>উৎসর্গ</w:t></w:r></w:p>\n");
                    String[] dedParas = dedicationText.replace("<br>", "\n").replaceAll("<[^>]*>", "").split("\n");
                    for (String para : dedParas) {
                        para = para.trim();
                        if (!para.isEmpty()) {
                            docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:after=\"0\" w:before=\"0\" w:line=\"240\" w:lineRule=\"auto\"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii=\"SolaimanLipi\" w:hAnsi=\"SolaimanLipi\" w:cs=\"SolaimanLipi\"/><w:sz w:val=\"28\"/><w:szCs w:val=\"28\"/></w:rPr><w:t>");
                            docXml.append(escapeXml(para));
                            docXml.append("</w:t></w:r></w:p>\n");
                        }
                    }
                    docXml.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n");
                }

                // ৪. সূচিপত্র (Table of Contents)
                if (contents.size() > 0) {
                    docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/><w:spacing w:before=\"800\" w:after=\"600\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"44\"/><w:szCs w:val=\"44\"/></w:rPr><w:t>সূচিপত্র</w:t></w:r></w:p>\n");
                    int chapterIndex = 1;
                    for (String[] section : contents) {
                        String chapterTitle = section[0];
                        if (chapterTitle != null && !chapterTitle.isEmpty()) {
                            docXml.append("<w:p><w:pPr><w:jc w:val=\"left\"/><w:ind w:left=\"720\"/><w:spacing w:after=\"200\"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii=\"SolaimanLipi\" w:hAnsi=\"SolaimanLipi\" w:cs=\"SolaimanLipi\"/><w:sz w:val=\"32\"/><w:szCs w:val=\"32\"/></w:rPr><w:t>");
                            docXml.append(escapeXml(chapterIndex + ".   " + chapterTitle));
                            docXml.append("</w:t></w:r></w:p>\n");
                            chapterIndex++;
                        }
                    }
                    docXml.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n");
                }

                // ৫. মূল কন্টেন্ট
                for (int i = 0; i < contents.size(); i++) {
                    String[] section = contents.get(i);
                    String chapterTitle = section[0];
                    String rawContent = section[1];

                    if (i > 0) {
                        docXml.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n");
                    }

                    if (chapterTitle != null && !chapterTitle.isEmpty()) {
                        docXml.append("<w:p><w:pPr><w:jc w:val=\"center\"/></w:pPr><w:r><w:rPr><w:b/><w:sz w:val=\"36\"/><w:szCs w:val=\"36\"/></w:rPr><w:t>");
                        docXml.append(escapeXml(chapterTitle));
                        docXml.append("</w:t></w:r></w:p>\n");
                        docXml.append("<w:p><w:r><w:t></w:t></w:r></w:p>\n"); 
                    }

                    if (rawContent == null) rawContent = "";
                    rawContent = rawContent.replace("<br>", "\n").replaceAll("<[^>]*>", "");
                    rawContent = rawContent.replaceAll("\n{3,}", "\n\n");
                    String[] paragraphs = rawContent.split("\n");

                    for (String para : paragraphs) {
                        para = para.trim();
                        if (!para.isEmpty()) {
                            docXml.append("<w:p><w:pPr><w:jc w:val=\"both\"/><w:spacing w:after=\"0\" w:before=\"0\" w:line=\"240\" w:lineRule=\"auto\"/><w:ind w:firstLine=\"720\"/></w:pPr><w:r><w:rPr><w:rFonts w:ascii=\"SolaimanLipi\" w:hAnsi=\"SolaimanLipi\" w:cs=\"SolaimanLipi\"/><w:sz w:val=\"28\"/><w:szCs w:val=\"28\"/></w:rPr><w:t>");
                            docXml.append(escapeXml(para));
                            docXml.append("</w:t></w:r></w:p>\n");
                        } else {
                            docXml.append("<w:p><w:pPr><w:spacing w:after=\"0\" w:before=\"0\" w:line=\"240\" w:lineRule=\"auto\"/></w:pPr><w:r><w:t></w:t></w:r></w:p>\n");
                        }
                    }
                }

                docXml.append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/></w:sectPr>\n");
                docXml.append("</w:body></w:document>");

                zos.write(docXml.toString().getBytes("UTF-8"));
                zos.closeEntry();
                zos.close();
                fos.close();

                activity.runOnUiThread(() -> Toast.makeText(activity, "আসল DOCX ফাইল তৈরি হয়েছে! এখন যেকোনো জায়গায় কপি-পেস্ট করা যাবে! 🎉", Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> Toast.makeText(activity, "DOCX তৈরি করতে সমস্যা হয়েছে!", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }
}
