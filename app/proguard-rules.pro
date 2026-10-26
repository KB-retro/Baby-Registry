# Reglas de ProGuard/R8 específicas de la app.

# PDFBox (lectura de PDF al importar) referencia clases de Java AWT que no existen en Android.
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.pdfbox.filter.JPXFilter
