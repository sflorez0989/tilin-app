# Rovika - notas del proyecto

## Que es
App Android que lee las notificaciones de pago (Nequi, Bancolombia, Daviplata, Davivienda, Nu, BBVA y otros), las dice en voz alta y lleva la cuenta del dia. Los pagos se guardan solo en el telefono. La app usa internet unicamente para buscar nuevas versiones en GitHub Releases.

## Donde esta todo
- El editor de GitHub corta lo que pasa de unas 2000 lineas, por eso el codigo esta partido en varios archivos de workflow:
  1. .github/workflows/pantallas.yml guarda MainActivity.kt (pantallas y tutorial).
  2. .github/workflows/extras.yml guarda Mas.kt y conecta la pestana Mas (modo manejo, gastos, resumen, exportar, acerca de).
  3. .github/workflows/extras2.yml guarda Mas2.kt y conecta salidas por confirmar y opciones extra.
  4. .github/workflows/build.yml crea todo lo demas (lectura de pagos, voz, letrero, sonido, widget, actualizaciones, iconos), arma el APK y lo publica en Releases.
- Si se vuelve a ejecutar pantallas.yml, hay que volver a ejecutar extras.yml y despues extras2.yml (cada uno conecta su parte).
- La carpeta app/src/main/java/com/tilin/app tiene el codigo.
- rovika.keystore es la firma fija. NO la borres: gracias a ella cada version nueva se instala encima de la anterior sin perder datos.
- La pestana Releases tiene cada version lista para descargar. Link fijo de la ultima: https://github.com/sflorez0989/tilin-app/releases/latest/download/Rovika.apk

## Como sacar una version nueva
1. Si cambian las pantallas: editar pantallas.yml, confirmar y esperar el check verde. Luego correr extras.yml y extras2.yml (Run workflow).
2. Si cambia otra cosa (lectura de pagos, letrero, voz, sonido, widget, actualizaciones): editar build.yml y subir VERSION_NAME arriba.
3. Confirmar cambios y esperar el check verde en Actions.
4. GitHub crea el release solo. Quien ya tiene la app recibe el aviso de actualizacion (maximo 12 horas) y la instala encima.
5. Cada archivo debe quedar por debajo de unas 1900 lineas para que el editor no lo corte.

## Como funcionan las actualizaciones
- El numero de version interno es 100 mas el numero de ejecucion de Actions. El release se llama v VERSION-bNUMERO y la app compara ese numero con el suyo.
- La app consulta https://api.github.com/repos/sflorez0989/tilin-app/releases/latest cada 12 horas.
- Android no permite instalar en silencio: la persona toca la notificacion, descarga el APK y toca Instalar.
- El repositorio debe ser publico para que esto funcione. Para pasarlo a privado hay que mover los APK a un repositorio publico aparte.

## Salidas (compras con tarjeta, envios, retiros)
- Cuando una notificacion de un banco habla de compra, envio, pago o retiro, Rovika la guarda en Salidas por confirmar. El usuario toca Contar (pasa a gastos) o Ignorar.
- Opciones extra permite contar las salidas automaticamente.
- GMF e impuestos se ignoran.
- Falta ver textos reales de notificaciones de compra para afinar las frases.

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

## Como seguir con Claude en un chat nuevo
Claude no recuerda chats anteriores. Pegar este archivo y el contenido de los archivos de workflow, y contarle que se quiere cambiar. Pedirle siempre el codigo completo para pegar, partido en varios archivos si hace falta.

## Preferencias del dueno
Conductor en Colombia, usa tablet Samsung sin computador e instala por APK. Prefiere pasos simples, codigo completo para pegar y mensajes cortos en espanol.

## Ideas pendientes
- Probar Bancolombia, Nu y Davivienda con notificaciones reales
- Afinar la deteccion de compras con tarjeta con textos reales
- Bloqueo con huella o PIN
- iPhone: Apple no permite leer notificaciones de otras apps, solo seria por correo
- Antes de vender: repositorio privado con repositorio publico aparte para los APK, politica de privacidad en linea y Google Play
