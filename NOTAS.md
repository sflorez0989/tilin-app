# Rovika - notas del proyecto

## Que es
App Android que lee las notificaciones de pago (Nequi, Bancolombia, Daviplata, Davivienda, Nu, BBVA y otros), las dice en voz alta y lleva la cuenta del dia. Los pagos se guardan solo en el telefono. La app usa internet unicamente para buscar nuevas versiones en GitHub Releases.

## Donde esta todo
- Este repositorio de GitHub guarda el codigo. El editor de GitHub corta lo que pasa de unas 2000 lineas, por eso el codigo esta partido en DOS archivos de workflow:
  1. .github/workflows/pantallas.yml guarda el archivo MainActivity.kt (las pantallas y el tutorial) dentro del repositorio.
  2. .github/workflows/build.yml crea todo lo demas, arma el APK y lo publica en Releases.
- La carpeta app/src/main/java/com/tilin/app tiene el codigo: MainActivity (pantallas y tutorial), PagoListenerService (lee notificaciones), Aviso (letrero), Voz, Apps y Actualizar (avisos de nueva version).
- rovika.keystore es la firma fija. NO la borres: gracias a ella cada version nueva se instala encima de la anterior sin perder datos.
- La pestana Releases tiene cada version lista para descargar. Link fijo de la ultima: https://github.com/sflorez0989/tilin-app/releases/latest/download/Rovika.apk

## Como sacar una version nueva
1. Si cambian las pantallas: editar pantallas.yml, confirmar y esperar el check verde. build.yml arranca solo cuando pantallas.yml termina bien.
2. Si cambia otra cosa (lectura de pagos, letrero, voz, actualizaciones): editar build.yml y subir VERSION_NAME arriba.
3. Confirmar cambios y esperar el check verde en Actions.
4. GitHub crea el release solo. Quien ya tiene la app recibe el aviso de actualizacion (maximo 12 horas) y la instala encima.
5. Cada archivo debe quedar por debajo de unas 1900 lineas para que el editor no lo corte.

## Como funcionan las actualizaciones
- El numero de version interno es 100 mas el numero de ejecucion de Actions. El release se llama v VERSION-bNUMERO y la app compara ese numero con el suyo.
- La app consulta https://api.github.com/repos/sflorez0989/tilin-app/releases/latest cada 12 horas (al abrirse y desde el servicio que lee notificaciones).
- Android no permite instalar en silencio: la persona toca la notificacion, descarga el APK y toca Instalar.
- El repositorio debe ser publico para que esto funcione. Para pasarlo a privado hay que mover los APK a un repositorio publico aparte.

## Reglas para no danar la app
- Un cambio a la vez y probar antes del siguiente.
- Si una compilacion sale en rojo, la app instalada sigue funcionando. Mandar captura del error.
- Si una compilacion sale verde pero dura menos de un minuto y no hay Artefactos, el archivo se corto al pegar. Revisar que la ultima linea del archivo sea la esperada.
- Para volver atras: en GitHub abrir el historial (History) del archivo y copiar una version anterior.
- No cambiar versiones de herramientas (Gradle 8.7, AGP 8.5.2, Kotlin 1.9.24, Java 17, ubuntu-24.04) sin necesidad.
- Limite de GitHub: el bloque run que crea los archivos no puede llevar expresiones dobles llave (de GitHub) porque pasaria el limite de 21000 caracteres. Por eso el numero de version se lee con la variable GITHUB_RUN_NUMBER.

## Como lee los pagos (textos reales vistos)
- Nequi por QR o Bre-B: Te enviaron plata por Bre-B Te enviaron $17.000. Entra a tu app y revisa tu saldo.
- Nequi entre cuentas: Envio NOMBRE te envio 7000, lo mejor! (sin signo de pesos).
- Recarga desde Bancolombia: texto de la notificacion aun sin ver, se detecta por la palabra recarga.
- Lo que el usuario envia, compras, GMF e impuestos se ignoran.

## Como seguir con Claude en un chat nuevo
Claude no recuerda chats anteriores. Pegar este archivo y el contenido de build.yml y pantallas.yml, y contarle que se quiere cambiar. Pedirle siempre el codigo completo para pegar, partido en dos archivos si hace falta.

## Preferencias del dueno
Conductor en Colombia, usa tablet Samsung sin computador e instala por APK. Prefiere pasos simples, codigo completo para pegar y mensajes cortos en espanol.

## Ideas pendientes
- Gastos con tarjeta (falta ver una notificacion real de compra)
- Resumen del mes y comparacion de semanas
- Modo manejo con letras grandes
- Sonido de caja registradora
- Bloqueo con huella o PIN
- Exportar a Excel
- Probar Bancolombia, Nu y Davivienda con notificaciones reales
- iPhone: Apple no permite leer notificaciones de otras apps, solo seria por correo
- Antes de vender: pasar el repositorio a privado (cambia el sistema de actualizaciones) y publicar en Google Play
