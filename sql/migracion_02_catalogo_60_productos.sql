/* Cielo Joyas | migracion_02_catalogo_60_productos
   Requisito: ejecutar primero sql/migracion_01_incio.sql en la misma base.
   En pgAdmin 4: abre Query Tool sobre maricielojoyas y ejecuta TODO este archivo.
   Añade 60 productos: 10 por categoría, 30 personalizables, 60 fotografías diferentes.
   Precios en soles: mayoría de piezas S/ 40 a S/ 50; collares desde S/ 70; máximo S/ 239.
   Precios y existencias de demostración. Fotografías externas de Caitlyn Minimalist,
   seleccionadas por diseño; no acreditan material 316L ni disponibilidad comercial.
   El campo material identifica estas piezas como muestras, sin certificados inventados.
   Cada fuente figura junto a su fila. URLs comprobadas al preparar esta migración.
   Puede repetirse: no duplica referencias, no modifica productos existentes ni repone stock.
   No crea usuarios, contraseñas, pedidos, ventas ni reseñas. */
BEGIN;

CREATE TEMP TABLE cielo_m02_catalogo (
    referencia VARCHAR(30) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(80) NOT NULL,
    descripcion TEXT NOT NULL,
    precio NUMERIC(12,2) NOT NULL CHECK (precio BETWEEN 40 AND 250),
    stock INTEGER NOT NULL CHECK (stock > 0),
    imagen_url TEXT NOT NULL UNIQUE,
    imagen_alt VARCHAR(254) NOT NULL,
    material VARCHAR(150) NOT NULL,
    personalizable BOOLEAN NOT NULL
) ON COMMIT DROP;

INSERT INTO cielo_m02_catalogo
(referencia,nombre,categoria,descripcion,precio,stock,imagen_url,imagen_alt,material,personalizable)
VALUES
    /* Fuente fotográfica: https://caitlynminimalist.com/products/toi-et-moi-dome-ring */
    ('ANI-02-01','Anillo Dúo de Color','Anillos','Banda de volumen suave con dos cristales de formas distintas. Puedes indicar tu talla. Fotografía ilustrativa de catálogo.',49,15,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/dazzling3-0044.jpg?v=1773701376&width=1200','Anillo Dúo de Color','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/rose-pink-art-deco-ring */
    ('ANI-02-02','Anillo Aurora Rosa','Anillos','Cristal rosa y silueta de inspiración art déco. Disponible para coordinar la talla. Fotografía ilustrativa de catálogo.',50,18,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_fullxfull.8708231074_7yfd.jpg?v=1792524952&width=1200','Anillo Aurora Rosa','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/amour-heart-signet-ring */
    ('ANI-02-03','Anillo Corazón Rosé','Anillos','Sello con corazón en relieve y pequeños destellos rosados. Personaliza la medida de la banda. Fotografía ilustrativa de catálogo.',48,21,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/9B2A0036.jpg?v=1792604733&width=1200','Anillo Corazón Rosé','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/heirloom-ruby-signet-ring */
    ('ANI-02-04','Anillo Rubí Vintage','Anillos','Cristal ovalado de tono rubí rodeado de un halo luminoso. Indica tu talla al elegirlo. Fotografía ilustrativa de catálogo.',50,24,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/9B2A0021.jpg?v=1792537655&width=1200','Anillo Rubí Vintage','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/half-jade-ring */
    ('ANI-02-05','Anillo Jade Bicolor','Anillos','Banda que combina una superficie verde lisa con un acabado metálico brillante. Elige la talla. Fotografía ilustrativa de catálogo.',45,12,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/mood11--6.jpg?v=1782757358&width=1200','Anillo Jade Bicolor','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/marquise-opal-diamond-ring */
    ('ANI-02-06','Anillo Nácar Marquesa','Anillos','Centro nacarado alargado con pequeños cristales laterales. Puedes solicitar tu medida. Fotografía ilustrativa de catálogo.',49,15,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_1588xN.5311645308_f5s7.jpg?v=1772143884&width=1200','Anillo Nácar Marquesa','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/emerald-cut-aquamarine-halo-ring */
    ('ANI-02-07','Anillo Brisa Aguamarina','Anillos','Cristal azul de corte rectangular y contorno de brillo delicado. Fotografía ilustrativa de catálogo.',50,18,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/SS40-2-10.jpg?v=1792087138&width=1200','Anillo Brisa Aguamarina','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/duo-knot-loop-ring-sample-sale-40 */
    ('ANI-02-08','Anillo Lazos Entrelazados','Anillos','Dos curvas entrelazadas forman una silueta ligera para combinar a diario. Fotografía ilustrativa de catálogo.',40,21,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/SS40-2-8.jpg?v=1792086532&width=1200','Anillo Lazos Entrelazados','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/ruby-heart-halo-ring-sample-sale-40 */
    ('ANI-02-09','Anillo Corazón Fucsia','Anillos','Centro fucsia con forma de corazón y un halo de pequeños cristales. Fotografía ilustrativa de catálogo.',45,24,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/SS40-2-9.jpg?v=1792086491&width=1200','Anillo Corazón Fucsia','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/pave-interlocking-ring-sample-sale-40 */
    ('ANI-02-10','Anillo Destellos Cruzados','Anillos','Aros entrelazados con detalle de pavé para una combinación luminosa. Fotografía ilustrativa de catálogo.',55,12,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/SS40-2-6.jpg?v=1792086262&width=1200','Anillo Destellos Cruzados','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/engraved-herringbone-name-necklace-all */
    ('COL-02-01','Collar Espiga con Nombre','Collares','Cadena plana tipo espiga. Añade un nombre o una fecha de hasta 20 caracteres. Fotografía ilustrativa de catálogo.',70,15,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_fullxfull.6601588622_d0ve.jpg?v=1738108442&width=1200','Collar Espiga con Nombre','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/pearl-initial-disc-necklace */
    ('COL-02-02','Collar Medalla Nácar Inicial','Collares','Medalla nacarada de contorno redondo. Elige una inicial para el grabado. Fotografía ilustrativa de catálogo.',75,18,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/stack3.jpg?v=1762455244&width=1200','Collar Medalla Nácar Inicial','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/pet-portrait-coin-necklace */
    ('COL-02-03','Collar Medalla Compañía','Collares','Medalla orgánica con motivo de mascota. Puedes añadir su nombre corto. Fotografía ilustrativa de catálogo.',70,21,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_fullxfull.6369974536_ehwc_d9f2d711-36b8-49d5-bd57-1a84e9e4d51a.jpg?v=1792965838&width=1200','Collar Medalla Compañía','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/baby-handprint-coin-necklace */
    ('COL-02-04','Collar Medalla Primer Recuerdo','Collares','Medalla con motivo de una pequeña huella. Personaliza con un nombre o una fecha. Fotografía ilustrativa de catálogo.',75,24,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_1588xN.6415552017_htns.jpg?v=1762456524&width=1200','Collar Medalla Primer Recuerdo','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/paw-print-coin-necklace */
    ('COL-02-05','Collar Medalla Patita','Collares','Colgante circular con una huella de patita. Puedes indicar un nombre para el grabado. Fotografía ilustrativa de catálogo.',70,12,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_fullxfull.6401102693_40xt.jpg?v=1762456468&width=1200','Collar Medalla Patita','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/round-cut-bezel-birthstone-necklace */
    ('COL-02-06','Collar Cristal Redondo','Collares','Cristal redondo en marco liso sobre una cadena fina. Coordina la medida de la cadena. Fotografía ilustrativa de catálogo.',70,15,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/lowNM195Thumbnails_0103.jpg?v=1793216806&width=1200','Collar Cristal Redondo','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/dainty-knot-pearl-necklace */
    ('COL-02-07','Collar Nudo de Perla','Collares','Un pequeño nudo metálico acompaña una perla colgante de silueta orgánica. Fotografía ilustrativa de catálogo.',70,18,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_fullxfull.5688408639_8l47.jpg?v=1782665270&width=1200','Collar Nudo de Perla','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/diamond-cross-necklace */
    ('COL-02-08','Collar Cruz de Luz','Collares','Cruz delicada cubierta de pequeños cristales sobre una cadena de brillo sutil. Fotografía ilustrativa de catálogo.',72,21,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_fullxfull.6805991337_p17q_3d936138-c284-4350-b7ce-c59c13654dea.jpg?v=1792797769&width=1200','Collar Cruz de Luz','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/dainty-jade-bar-necklace */
    ('COL-02-09','Collar Barra Verde','Collares','Colgante alargado de color verde sobre una cadena fina, de estilo sereno. Fotografía ilustrativa de catálogo.',70,24,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_1588xN.5282222073_2cda.jpg?v=1782665185&width=1200','Collar Barra Verde','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/marquise-cut-bezel-birthstone-necklace */
    ('COL-02-10','Collar Marquesa Rosada','Collares','Cristal de corte marquesa engastado en un marco sencillo, para llevar solo o en capas. Fotografía ilustrativa de catálogo.',85,12,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/lowNM195Thumbnails_0106.jpg?v=1793217316&width=1200','Collar Marquesa Rosada','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/carrie-initial-bracelet */
    ('PUL-02-01','Pulsera Inicial Delicada','Pulseras','Cadena fina con una inicial como protagonista. Escribe la letra que deseas. Fotografía ilustrativa de catálogo.',45,15,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/CM2_5164-Edit-3.jpg?v=1762447436&width=1200','Pulsera Inicial Delicada','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/newsletter-initial-bracelet */
    ('PUL-02-02','Pulsera Inicial Editorial','Pulseras','Inicial de trazo marcado sobre una cadena ligera. Personaliza con tu letra favorita. Fotografía ilustrativa de catálogo.',45,18,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/CM2_5086-Edit.jpg?v=1762447436&width=1200','Pulsera Inicial Editorial','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/strap-bracelet */
    ('PUL-02-03','Pulsera Trama Dorada','Pulseras','Cadena compacta con textura de eslabones entrelazados. Indica la medida de tu muñeca. Fotografía ilustrativa de catálogo.',50,21,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/BR090-2.jpg?v=1742250440&width=1200','Pulsera Trama Dorada','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/mini-mariner-chain-bracelet */
    ('PUL-02-04','Pulsera Eslabón Marino','Pulseras','Eslabones pequeños de inspiración marinera. Coordina el largo de la cadena. Fotografía ilustrativa de catálogo.',45,24,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/Costal5.jpg?v=1757000822&width=1200','Pulsera Eslabón Marino','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/pink-tourmaline-heart-tennis-bracelet */
    ('PUL-02-05','Pulsera Corazones Rosa','Pulseras','Hilera de cristales rosados con forma de corazón. Puedes elegir la medida. Fotografía ilustrativa de catálogo.',50,12,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/9B2A0087-2.jpg?v=1792605549&width=1200','Pulsera Corazones Rosa','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/strap-bracelet-silver */
    ('PUL-02-06','Pulsera Trama Plateada','Pulseras','Una cadena de eslabones compactos de tono plateado para combinar en capas. Personaliza el largo. Fotografía ilustrativa de catálogo.',48,15,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/LowGILDED_DREAMS-3482.jpg?v=1792443464&width=1200','Pulsera Trama Plateada','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/diamond-star-chain-bracelet-sample-sale-41 */
    ('PUL-02-07','Pulsera Estrella de Luz','Pulseras','Estrellas con cristales enlazadas en una pulsera de acabado luminoso. Fotografía ilustrativa de catálogo.',42,18,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/SS41-2-16.jpg?v=1792624737&width=1200','Pulsera Estrella de Luz','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/gold-star-chain-bracelet-sample-sale-41 */
    ('PUL-02-08','Pulsera Constelación Dorada','Pulseras','Una hilera de estrellas lisas para un look ligero y sencillo. Fotografía ilustrativa de catálogo.',40,21,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/SS41-2-2.jpg?v=1792624700&width=1200','Pulsera Constelación Dorada','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/crimson-bordeaux-enamel-bangle-sample-sale-41 */
    ('PUL-02-09','Brazalete Burdeos','Pulseras','Brazalete de acento rojo oscuro y silueta pulida, ideal como pieza central. Fotografía ilustrativa de catálogo.',50,24,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/SS41-2-40.jpg?v=1792624610&width=1200','Brazalete Burdeos','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/open-turquoise-stone-bangle-sample-sale-41 */
    ('PUL-02-10','Brazalete Turquesa Abierto','Pulseras','Diseño abierto con acentos de color turquesa y estructura de acabado metálico. Fotografía ilustrativa de catálogo.',55,12,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/SS41-2-4.jpg?v=1792624585&width=1200','Brazalete Turquesa Abierto','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/round-diamond-studs */
    ('ARE-02-01','Aretes Cristal Redondo','Aretes','Par de pendientes con cristal redondo y un engaste sencillo para el día a día. Fotografía ilustrativa de catálogo.',40,15,'https://cdn.shopify.com/s/files/1/0255/2207/7770/products/il_1588xN.3209423180_c4zi.jpg?v=1762443768&width=1200','Aretes Cristal Redondo','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/moonstone-studs-all */
    ('ARE-02-02','Aretes Luz de Luna','Aretes','Par de pendientes con centros de brillo lechoso y silueta redondeada. Fotografía ilustrativa de catálogo.',45,18,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/silver--5_ec6bffb6-1c22-4941-9a18-1983168bb168.jpg?v=1793156505&width=1200','Aretes Luz de Luna','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/lily-studs */
    ('ARE-02-03','Aretes Flor de Loto','Aretes','Pequeñas flores de loto con cristales que aportan un destello delicado. Fotografía ilustrativa de catálogo.',42,21,'https://cdn.shopify.com/s/files/1/0255/2207/7770/products/il_1588xN.2534655571_dp9l.jpg?v=1762442201&width=1200','Aretes Flor de Loto','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/dainty-sphere-beaded-huggies */
    ('ARE-02-04','Argollas Mini Esferas','Aretes','Argollas pequeñas con textura de esferas. Consulta la medida que prefieres. Fotografía ilustrativa de catálogo.',45,24,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/VANEDITED-7_e4ecb918-1776-4d9a-b425-eaf0d1062ddb.jpg?v=1792695358&width=1200','Argollas Mini Esferas','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/clear-moonstone-huggies */
    ('ARE-02-05','Argollas Nácar Claro','Aretes','Argollas con centros claros de efecto nacarado. Coordina el diámetro para tu ajuste. Fotografía ilustrativa de catálogo.',50,12,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_fullxfull.6111725906_8gml_1.webp?v=1792693099&width=1200','Argollas Nácar Claro','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/trio-sphere-studs-1 */
    ('ARE-02-06','Aretes Trío de Esferas','Aretes','Tres pequeñas esferas unidas en un pendiente minimalista de brillo suave. Fotografía ilustrativa de catálogo.',40,15,'https://cdn.shopify.com/s/files/1/0255/2207/7770/products/il_1588xN.3706271317_oihm.jpg?v=1762444859&width=1200','Aretes Trío de Esferas','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/astra-moon-star-huggies-1 */
    ('ARE-02-07','Argollas Luna y Estrella','Aretes','Un par asimétrico de argollas con dijes de luna y estrella. Fotografía ilustrativa de catálogo.',48,18,'https://cdn.shopify.com/s/files/1/0255/2207/7770/products/il_1588xN.4249106218_5wn3.jpg?v=1762445877&width=1200','Argollas Luna y Estrella','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/rose-pink-art-deco-huggies */
    ('ARE-02-08','Argollas Aurora Rosa','Aretes','Argollas pequeñas con cristales rosados e inspiración vintage. Fotografía ilustrativa de catálogo.',50,21,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_fullxfull.8707902288_o77b.jpg?v=1792522598&width=1200','Argollas Aurora Rosa','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/angelina-pearl-studs */
    ('ARE-02-09','Aretes Perla Serena','Aretes','Perlas de silueta clásica sobre un soporte discreto, pensadas para combinar fácilmente. Fotografía ilustrativa de catálogo.',45,24,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/LowCM1_9973.jpg?v=1792436186&width=1200','Aretes Perla Serena','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/diamond-pearl-ear-jacket */
    ('ARE-02-10','Aretes Doble Destello','Aretes','Diseño con cristales y perlas en dos planos para enmarcar el lóbulo. Fotografía ilustrativa de catálogo.',55,12,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/il_fullxfull.5945633522_ikkk.jpg?v=1762450154&width=1200','Aretes Doble Destello','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/duo-bead-chain-anklet-18k-gold */
    ('TOB-02-01','Tobillera Doble Cadena Dorada','Tobilleras','Dos cadenas finas con pequeñas cuentas. Indica la medida de tu tobillo. Fotografía ilustrativa de catálogo.',45,15,'https://caitlynminimalist.com/cdn/shop/files/il_1588xN.3073384114_9vph.jpg?v=1763583234&width=1200','Tobillera Doble Cadena Dorada','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/micro-rolo-chain-anklet-18k-gold */
    ('TOB-02-02','Tobillera Rolo Dorada','Tobilleras','Cadena de pequeños eslabones redondos con acabado dorado. Personaliza el largo. Fotografía ilustrativa de catálogo.',40,18,'https://caitlynminimalist.com/cdn/shop/files/il_1588xN.5305172854_ed1y.jpg?v=1762447599&width=1200','Tobillera Rolo Dorada','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/micro-rolo-chain-anklet-sterling-silver */
    ('TOB-02-03','Tobillera Rolo Plateada','Tobilleras','Eslabones redondos de tono plateado para una combinación discreta. Elige el largo. Fotografía ilustrativa de catálogo.',40,21,'https://caitlynminimalist.com/cdn/shop/files/il_1588xN.5353284405_czu2.jpg?v=1748545015&width=1200','Tobillera Rolo Plateada','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/dainty-pearl-beaded-anklet */
    ('TOB-02-04','Tobillera Perlas y Cuentas','Tobilleras','Pequeñas perlas alternadas con cuentas metálicas. Coordina la medida a tu gusto. Fotografía ilustrativa de catálogo.',48,24,'https://caitlynminimalist.com/cdn/shop/files/il_1588xN.5055257479_qemt.jpg?v=1686937415&width=1200','Tobillera Perlas y Cuentas','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/herringbone-anklet */
    ('TOB-02-05','Tobillera Espiga Dorada','Tobilleras','Cadena plana de eslabones compactos y brillo continuo. Puedes indicar el largo. Fotografía ilustrativa de catálogo.',45,12,'https://caitlynminimalist.com/cdn/shop/products/il_fullxfull.2534483069_1uft.jpg?v=1762442180&width=1200','Tobillera Espiga Dorada','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/duo-bead-chain-anklet-sterling-silver */
    ('TOB-02-06','Tobillera Doble Cadena Plateada','Tobilleras','Doble cadena de acabado plateado con cuentas pequeñas y un cierre delicado. Fotografía ilustrativa de catálogo.',45,15,'https://caitlynminimalist.com/cdn/shop/files/BR016anklet-9482_6339e010-721d-4dbd-a013-e67adc7cef30.jpg?v=1778623912&width=1200','Tobillera Doble Cadena Plateada','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/eternity-pearl-anklet-white-gold */
    ('TOB-02-07','Tobillera Perlas Eternas Plateada','Tobilleras','Perlas blancas distribuidas entre eslabones de tono plateado. Fotografía ilustrativa de catálogo.',50,18,'https://caitlynminimalist.com/cdn/shop/files/il_fullxfull.7119453262_nkvv.jpg?v=1756503356&width=1200','Tobillera Perlas Eternas Plateada','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/eternity-pearl-anklet-18k-gold */
    ('TOB-02-08','Tobillera Perlas Eternas Dorada','Tobilleras','Una hilera de perlas pequeñas y eslabones dorados para un detalle luminoso. Fotografía ilustrativa de catálogo.',50,21,'https://caitlynminimalist.com/cdn/shop/files/il_fullxfull.7167418631_46r9.jpg?v=1755556848&width=1200','Tobillera Perlas Eternas Dorada','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/brinley-pearl-anklet */
    ('TOB-02-09','Tobillera Perlas Mini Dorada','Tobilleras','Hilera delicada de pequeñas perlas con cadena de extensión dorada. Fotografía ilustrativa de catálogo.',48,24,'https://caitlynminimalist.com/cdn/shop/products/il_1588xN.4103719929_s8uo.jpg?v=1756420277&width=1200','Tobillera Perlas Mini Dorada','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/brinley-pearl-anklet-sterling-silver */
    ('TOB-02-10','Tobillera Perlas Mini Plateada','Tobilleras','Pequeñas perlas de brillo suave y terminación de tono plateado. Fotografía ilustrativa de catálogo.',48,12,'https://caitlynminimalist.com/cdn/shop/products/il_1588xN.4103731237_3b0l.jpg?v=1756420277&width=1200','Tobillera Perlas Mini Plateada','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/bubble-letter-jewelry-set */
    ('SET-02-01','Set Inicial Burbuja','Sets','Collar y aretes con una inicial de volumen suave. Elige una letra para el conjunto. Fotografía ilustrativa de catálogo.',120,15,'https://cdn.shopify.com/s/files/1/0255/2207/7770/files/lowmb.jpg?v=1792618255&width=1200','Set Inicial Burbuja','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/heart-station-necklace-bracelet-set */
    ('SET-02-02','Set Corazones en Cadena','Sets','Collar y pulsera con pequeños corazones. Coordina el largo de ambas cadenas. Fotografía ilustrativa de catálogo.',130,18,'https://caitlynminimalist.com/cdn/shop/files/il_1588xN.6618192096_1eud.jpg?v=1762550423&width=1200','Set Corazones en Cadena','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/dainty-diamond-tennis-necklace-bracelet-set */
    ('SET-02-03','Set Línea de Luz','Sets','Collar y pulsera de cristales en una hilera delicada. Personaliza las medidas. Fotografía ilustrativa de catálogo.',160,21,'https://caitlynminimalist.com/cdn/shop/files/BundleCollection--3.jpg?v=1762551462&width=1200','Set Línea de Luz','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/diamond-bezel-tennis-necklace-bracelet-set */
    ('SET-02-04','Set Cristales en Marco','Sets','Collar y pulsera con cristales rodeados de marcos lisos. Coordina sus largos. Fotografía ilustrativa de catálogo.',195,24,'https://caitlynminimalist.com/cdn/shop/files/stack2-2.jpg?v=1762461246&width=1200','Set Cristales en Marco','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/art-deco-diamond-earrings-ring-set */
    ('SET-02-05','Set Destello Art Déco','Sets','Aretes y anillo de inspiración geométrica vintage. Puedes elegir la talla del anillo. Fotografía ilustrativa de catálogo.',145,12,'https://caitlynminimalist.com/cdn/shop/files/il_fullxfull.7389910995_24tr.jpg?v=1762539188&width=1200','Set Destello Art Déco','Metal y detalles decorativos; muestra de catálogo',TRUE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/pearl-knot-necklace-earrings-set */
    ('SET-02-06','Set Nudo y Perlas','Sets','Collar y aretes con motivo de nudo y perlas, combinados para una ocasión especial. Fotografía ilustrativa de catálogo.',110,15,'https://caitlynminimalist.com/cdn/shop/files/il_fullxfull.7378142359_srss.jpg?v=1762547175&width=1200','Set Nudo y Perlas','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/marquise-diamond-necklace-earrings-set */
    ('SET-02-07','Set Hojas de Luz','Sets','Collar y aretes con cristales alargados en una composición de pequeñas hojas. Fotografía ilustrativa de catálogo.',125,18,'https://caitlynminimalist.com/cdn/shop/files/NR055_ER154.jpg?v=1761252062&width=1200','Set Hojas de Luz','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/diamond-butterfly-drop-jewelry-set */
    ('SET-02-08','Set Mariposas de Cristal','Sets','Collar con mariposa y aretes colgantes con motivos coordinados. Fotografía ilustrativa de catálogo.',135,21,'https://caitlynminimalist.com/cdn/shop/files/il_fullxfull.7318134366_4nvl.jpg?v=1761634789&width=1200','Set Mariposas de Cristal','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/3-prong-diamond-tennis-necklace-bracelet-set */
    ('SET-02-09','Set Gala de Cristales','Sets','Collar y pulsera con una hilera continua de cristales de brillo intenso. Fotografía ilustrativa de catálogo.',239,24,'https://caitlynminimalist.com/cdn/shop/files/il_fullxfull.7341894430_qsam.jpg?v=1762645022&width=1200','Set Gala de Cristales','Metal y detalles decorativos; muestra de catálogo',FALSE),
    /* Fuente fotográfica: https://caitlynminimalist.com/products/diamond-bow-necklace-earrings-set */
    ('SET-02-10','Set Lazos de Luz','Sets','Collar y aretes con delicados lazos cubiertos de pequeños cristales. Fotografía ilustrativa de catálogo.',125,12,'https://caitlynminimalist.com/cdn/shop/files/il_fullxfull.7341854930_1tpn.jpg?v=1762461982&width=1200','Set Lazos de Luz','Metal y detalles decorativos; muestra de catálogo',FALSE);

DO $$
BEGIN
    IF (SELECT count(*) FROM cielo_m02_catalogo) <> 60
       OR (SELECT count(*) FROM cielo_m02_catalogo WHERE personalizable) <> 30
       OR (SELECT count(DISTINCT split_part(imagen_url,'?',1)) FROM cielo_m02_catalogo) <> 60
       OR (SELECT count(DISTINCT categoria) FROM cielo_m02_catalogo) <> 6
       OR EXISTS (SELECT 1 FROM cielo_m02_catalogo GROUP BY categoria HAVING count(*) <> 10) THEN
        RAISE EXCEPTION 'Catálogo inválido: se requieren 60 productos, 30 personalizables, 60 fotos y 10 piezas por categoría.';
    END IF;
    IF to_regclass('public.productos') IS NULL OR to_regclass('public.categorias') IS NULL THEN
        RAISE EXCEPTION 'Primero ejecuta migracion_01_incio.sql en esta base de datos.';
    END IF;
END $$;

INSERT INTO categorias(nombre)
SELECT DISTINCT categoria FROM cielo_m02_catalogo
ON CONFLICT (nombre) DO NOTHING;

/* DO NOTHING preserva precios, stock y ediciones manuales si repites el archivo. */
INSERT INTO productos
(referencia,nombre,descripcion,categoria_id,precio,stock,etiqueta,imagen_url,imagen_alt,material,personalizable,activo)
SELECT d.referencia,d.nombre,d.descripcion,c.id,d.precio,d.stock,
       CASE WHEN d.personalizable THEN 'PERSONALIZABLE' ELSE 'COLECCIÓN CIELO' END,
       d.imagen_url,d.imagen_alt,d.material,d.personalizable,TRUE
FROM cielo_m02_catalogo d
JOIN categorias c ON c.nombre=d.categoria
ON CONFLICT (referencia) DO NOTHING;

DO $$
BEGIN
    IF (SELECT count(*) FROM cielo_m02_catalogo d JOIN productos p USING (referencia)) <> 60 THEN
        RAISE EXCEPTION 'No se incorporaron los 60 productos. Se revierte esta migración.';
    END IF;
END $$;

/* Resultado esperado al instalar sobre migración 01: 68 productos en total.
   Esta consulta muestra SOLO los 60 de migración 02: seis filas de 10 productos. */
SELECT c.nombre AS categoria, count(*) AS productos_nuevos,
       count(*) FILTER (WHERE p.personalizable) AS personalizables,
       min(p.precio) AS precio_minimo, max(p.precio) AS precio_maximo
FROM productos p
JOIN categorias c ON c.id=p.categoria_id
JOIN cielo_m02_catalogo d ON d.referencia=p.referencia
GROUP BY c.nombre ORDER BY c.nombre;

COMMIT;

-- Preferencias de entrega editables, independientes del historial de pedidos.
CREATE TABLE IF NOT EXISTS usuario_entrega (
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    campo VARCHAR(30) NOT NULL,
    valor VARCHAR(254),
    PRIMARY KEY (usuario_id, campo)
);
