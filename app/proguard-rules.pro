# Regras R8 do TurnoTrack.
# Room, Compose, DataStore e kotlinx.serialization já trazem as suas próprias regras.
# O JSON (backup e Gemini) é tratado com JsonElement, sem reflexão, por isso não são precisas regras extra.

# Mantém números de linha nos stack traces para facilitar o diagnóstico de erros.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
