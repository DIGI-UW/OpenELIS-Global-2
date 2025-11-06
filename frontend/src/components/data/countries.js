const nationalityDetails = [
  {
    label: "COSTA RICA",
    value: "COSTA RICAN",
  },
  {
    label: "CROATIA",
    value: "CROTIAN",
  },
  {
    label: "CUBA",
    value: "CUBAN",
  },
  {
    label: "CYPRUS",
    value: "CYPRIOT",
  },
  {
    label: "CZECH REPUBLIC",
    value: "CZECH",
  },
  {
    label: "NORTH KOREA DEM PEOPLES",
    value: "NORTH KOREAN",
  },
  {
    label: "REPUBLIC OF CONGO",
    value: "CONGOLESE (BRAZ)",
  },
  {
    label: "DENMARK",
    value: "DANE",
  },
  {
    label: "DJIBOUTI",
    value: "DHIBOUTI",
  },
  {
    label: "DOMINICA",
    value: "DOMINICAN",
  },
  {
    label: "EGYPT",
    value: "EGYPTIAN",
  },
  {
    label: "EL SALVADOR",
    value: "SALVADORAN",
  },
  {
    label: "EQUATORIAL GUINEA",
    value: "EQUATORIAL GUINEA",
  },
  {
    label: "ERITREA",
    value: "ERITREAN",
  },
  {
    label: "ESTONIA",
    value: "ESTONIAN",
  },
  {
    label: "PAKISTAN",
    value: "PAKISTANI",
  },
  {
    label: "PALAU",
    value: "PALAUAN",
  },
  {
    label: "PAPUA NEW GUINEA",
    value: "PAPUA NEW GUINEAN",
  },
  {
    label: "PARAGUAY",
    value: "PARAGUAYAN",
  },
  {
    label: "PERU",
    value: "PERUVIAN",
  },
  {
    label: "PHILIPPINES",
    value: "FILIPINO",
  },
  {
    label: "PITCAIRN",
    value: "PITCAIRN",
  },
  {
    label: "POLAND",
    value: "POLE",
  },
  {
    label: "PUERTO RICO",
    value: "PUERTO RICAN",
  },
  {
    label: "QATAR",
    value: "QATAR",
  },
  {
    label: "REPUBLIC OF MOLDOVA",
    value: "MOLDOVAN",
  },
  {
    label: "REPUBLIC OF KOREA  SOUTH",
    value: "SOUTH KOREAN",
  },
  {
    label: "RUSSIAN FEDERATION",
    value: "RUSSIAN",
  },
  {
    label: "RUWANDA",
    value: "RWANDAN",
  },
  {
    label: "ST. HELENA",
    value: "ST. HELENA",
  },
  {
    label: "SAINT KITTS AND NEVIS",
    value: "SAINT KITTS AND NEVIS",
  },
  {
    label: "SAINT VINCENT GRENADIN",
    value: "SAINT VINCENT GRENADINES",
  },
  {
    label: "SAMOA",
    value: "SAMOAN",
  },
  {
    label: "SAN MARINO",
    value: "SAN MARINO",
  },
  {
    label: "SAO TOME AND PRINCIPE",
    value: "SAO TOME AND PRINCIPE",
  },
  {
    label: "SAUDI ARABIA",
    value: "SAUDI",
  },
  {
    label: "SENEGAL",
    value: "SENEGALESE",
  },
  {
    label: "SEYCHELLES",
    value: "SEYCHELLES",
  },
  {
    label: "SINGAPORE",
    value: "SINGAPOREAN",
  },
  {
    label: "SLOVAKIA",
    value: "SLOVAK",
  },
  {
    label: "SOLOMON  ISLANDS",
    value: "SOLOMON ISLANDS",
  },
  {
    label: "SOMALIA",
    value: "SOMALI",
  },
  {
    label: "SOUTH AFRICA",
    value: "SOUTH AFRICAN",
  },
  {
    label: "SOUTH GEORGIA SANWICH ISL",
    value: "SOUTH GEORGIA SANWICH ISL",
  },
  {
    label: "SPAIN",
    value: "SPANISH",
  },
  {
    label: "SUDAN",
    value: "SUDANESE",
  },
  {
    label: "SVALBARD JAN MEYEN ISLAND",
    value: "SVALBARD JAN MEYEN ISLAND",
  },
  {
    label: "SWAZILAND",
    value: "SWAZILAND",
  },
  {
    label: "SWEDEN",
    value: "SWEDE",
  },
  {
    label: "SWITZERLAND",
    value: "SWISS",
  },
  {
    label: "TAIWAN, PROVINCE OF CHINA",
    value: "TAIWANESE",
  },
  {
    label: "TAJIKISTAN",
    value: "TAJIKISTANI",
  },
  {
    label: "THAILAND",
    value: "THAI",
  },
  {
    label: "REP OF NORTH MACEDONIA",
    value: "MACEDONIAN",
  },
  {
    label: "TOGO",
    value: "TOGO",
  },
  {
    label: "TOKELAU",
    value: "TOKELAU",
  },
  {
    label: "TUNISIA",
    value: "TUNISIAN",
  },
  {
    label: "TURKEY",
    value: "TURK",
  },
  {
    label: "TURKS AND CAICOS ISLANDS",
    value: "TURKS AND CAICOS ISLANDS",
  },
  {
    label: "TUVALU",
    value: "TUVALUAN",
  },
  {
    label: "UGANDA",
    value: "UGANDAN",
  },
  {
    label: "UKRAINE",
    value: "UKRAINIAN",
  },
  {
    label: "UNITED ARAB EMIRATES",
    value: "EMIRATI",
  },
  {
    label: "UNITED KINGDOM",
    value: "BRITISH - CITIZEN",
  },
  {
    label: "UK - DEPENDANT",
    value: "BRITISH - DEPENDANT",
  },
  {
    label: "UK - NATIONAL",
    value: "BRITISH - NATIONAL",
  },
  {
    label: "UK - OVERSEAS CITIZEN",
    value: "BRITISH- OVERSEAS CITIZEN",
  },
  {
    label: "UK -SUBJECT",
    value: "BRITISH - SUBJECT",
  },
  {
    label: "UNITED STATES",
    value: "AMERICAN",
  },
  {
    label: "US MINOR OUTLYING ISLANDS",
    value: "US MINOR OUTLYING ISLANDS",
  },
  {
    label: "URUGUAY",
    value: "URUGUAYAN",
  },
  {
    label: "UZBEKISTAN",
    value: "UZBEKISTANI",
  },
  {
    label: "VANUATU",
    value: "VANUATU",
  },
  {
    label: "VATICAN CITY STATE",
    value: "VATICAN",
  },
  {
    label: "VIETNAM",
    value: "VIETNAMESE",
  },
  {
    label: "VIRGIN ISLANDS (BRITISH)",
    value: "VIRGIN ISLANDS (BRITISH)",
  },
  {
    label: "VIRGIN ISLANDS (US)",
    value: "VIRGIN ISLANDS (US)",
  },
  {
    label: "WALLIS AND FUTUNA ISLANDS",
    value: "WALLIS AND FUTUNA ISLANDS",
  },
  {
    label: "WESTERN SAHARA",
    value: "WESTERN SAHARA",
  },
  {
    label: "YEMEN",
    value: "YEMENI",
  },
  {
    label: "ZAMBIA",
    value: "ZAMBIAN",
  },
  {
    label: "ZIMBABWE ZWE",
    value: "ZIMBABWEAN ZWE",
  },
  {
    label: "ETHIOPIA",
    value: "ETHIOPIAN",
  },
  {
    label: "FALKLAND ISLANDS",
    value: "FALKLAND ISLANDS",
  },
  {
    label: "FAROE ISLANDS",
    value: "FAROE ISLANDS",
  },
  {
    label: "FIJI",
    value: "FIJIAN",
  },
  {
    label: "FINLAND",
    value: "FINN",
  },
  {
    label: "FRANCE",
    value: "FRENCH",
  },
  {
    label: "FRANCE, METROPOLITAN",
    value: "FRANCE, METROPOLITAN",
  },
  {
    label: "FRENCH GUYIANA",
    value: "GUIANESE (FRENCH)",
  },
  {
    label: "FRENCH POLYNESIA",
    value: "FRENCH POLYNESIAN",
  },
  {
    label: "FRENCH SOUTHERN TERRITORY",
    value: "FRENCH SOUTHERN TERRITORY",
  },
  {
    label: "GABON",
    value: "GABON",
  },
  {
    label: "GAMBIA",
    value: "GAMBIAN",
  },
  {
    label: "GEORGIA",
    value: "GEORGIAN",
  },
  {
    label: "GHANA",
    value: "GHANAIAN",
  },
  {
    label: "GIBRALTA",
    value: "GIBRALTAR",
  },
  {
    label: "GREECE",
    value: "GREEK",
  },
  {
    label: "GREENLAND",
    value: "GREENLAND",
  },
  {
    label: "GRENADA",
    value: "GRENADIAN",
  },
  {
    label: "GUADELOUPE",
    value: "GUADELOUPE",
  },
  {
    label: "GUAM",
    value: "GUAM",
  },
  {
    label: "GUATEMALA",
    value: "GUATEMALA",
  },
  {
    label: "GUINEA",
    value: "GUINEAN",
  },
  {
    label: "GUINEA - BISSAU",
    value: "GUINEA - BISSAU NATIONAL",
  },
  {
    label: "GUYANA",
    value: "GUYANESE",
  },
  {
    label: "HAITI",
    value: "HAITIAN",
  },
  {
    label: "HEARD AND MCDONALD ISLAND",
    value: "HEARD AND MCDONALD ISLAND",
  },
  {
    label: "HONDURAS",
    value: "HONDURAN",
  },
  {
    label: "HUNGARY",
    value: "HUNGARIAN",
  },
  {
    label: "ICELAND",
    value: "ICELANDER",
  },
  {
    label: "INDONESIA",
    value: "INDONESIAN",
  },
  {
    label: "IRAN",
    value: "IRANIAN",
  },
  {
    label: "IRAQ",
    value: "IRAQI",
  },
  {
    label: "IRELAND",
    value: "IRISH",
  },
  {
    label: "ISRAEL",
    value: "ISRAELI",
  },
  {
    label: "JAPAN",
    value: "JAPANESE",
  },
  {
    label: "KENYA",
    value: "KENYAN",
  },
  {
    label: "KIRIBATI",
    value: "KIRIBATIAN",
  },
  {
    label: "KUWAIT",
    value: "KUWAITI",
  },
  {
    label: "KYRGYZSTAN",
    value: "KYRGYSTANI",
  },
  {
    label: "LATVIA",
    value: "LATVIAN",
  },
  {
    label: "LEBANON",
    value: "LEBANESE",
  },
  {
    label: "LESOTHO",
    value: "LESOTHO",
  },
  {
    label: "LIBERIA",
    value: "LIBERIAN",
  },
  {
    label: "LITHUANIA",
    value: "LITHUANIAN",
  },
  {
    label: "MACAU",
    value: "MACAU",
  },
  {
    label: "MADAGASCAR",
    value: "MADAGASCAR",
  },
  {
    label: "MALAWI",
    value: "MALAWIAN",
  },
  {
    label: "MALAYSIA",
    value: "MALAYSIAN",
  },
  {
    label: "MALDIVES",
    value: "MALDIVIAN",
  },
  {
    label: "MARSHALL ISLANDS",
    value: "MARSHALL ISLANDS",
  },
  {
    label: "MARTINIQUE",
    value: "MARTINIQUE",
  },
  {
    label: "MAURITANIA",
    value: "MAURITANIAN",
  },
  {
    label: "MAURITIUS",
    value: "MAURITIAN",
  },
  {
    label: "MAYOTTE",
    value: "MAYOTTE",
  },
  {
    label: "MEXICO",
    value: "MEXICAN",
  },
  {
    label: "US PACIFIC ISLANDS",
    value: "US PACIFIC ISLANDS",
  },
  {
    label: "UPPER VOLTA",
    value: "UPPER VOLTA",
  },
  {
    label: "MAURITIUS (ISLAND)",
    value: "MAURITIUS (ISLAND)",
  },
  {
    label: "DIEGO GARCIA (MTIUS)",
    value: "DIEGO GARCIA (MTIUS)",
  },
  {
    label: "PEROS BANHOS (MTIUS)",
    value: "PEROS BANHOS (MTIUS)",
  },
  {
    label: "STATELESS",
    value: "STATELESS",
  },
  {
    label: "YEMEN DEMOCRATIC",
    value: "YEMEN DEMOCRATIC",
  },
  {
    label: "CANTON  I",
    value: "CANTON  I",
  },
  {
    label: "NOT SPECIFIED",
    value: "NOT SPECIFIED",
  },
  {
    label: "PACIFIC ISLANDS",
    value: "PACIFIC ISLANDS",
  },
  {
    label: "HIGH SEAS",
    value: "HIGH SEAS",
  },
  {
    label: "RODRIGUES (MTIUS)",
    value: "RODRIGUES (MTIUS)",
  },
  {
    label: "JOHNSTON ISLAND",
    value: "JOHNSTON ISLAND",
  },
  {
    label: "KAMPUCHEA",
    value: "KAMPUCHEA",
  },
  {
    label: "ST PIERRE ",
    value: "ST PIERRE ",
  },
  {
    label: "AGALEGA (MTIUS)",
    value: "AGALEGA (MTIUS)",
  },
  {
    label: "DOMINICAN REPUBLIC",
    value: "DOMINICAN REPUBLIC",
  },
  {
    label: "DRONNING MAUD LAND",
    value: "DRONNING MAUD LAND",
  },
  {
    label: "GERMANY, FED. REP OF",
    value: "GERMANY, FED. REP OF",
  },
  {
    label: "CZECHOSLOVAKIA",
    value: "CZECHOSLOVAKIA",
  },
  {
    label: "ST. BRANDON (MTIUS)",
    value: "ST. BRANDON (MTIUS)",
  },
  {
    label: "BYELORUSSIAN SSR",
    value: "BYELORUSSIAN SSR",
  },
  {
    label: "ST. LUCIA",
    value: "ST. LUCIA",
  },
  {
    label: "USSR",
    value: "USSR",
  },
  {
    label: "ZAIRE",
    value: "ZAIRE",
  },
  {
    label: "WAKE ISLAND",
    value: "WAKE ISLAND",
  },
  {
    label: "MADEIRA ISLANDS",
    value: "MADEIRA ISLANDS",
  },
  {
    label: "MOSCOW",
    value: "MOSCOW",
  },
  {
    label: "MOSCOW",
    value: "MOSCOW",
  },
  {
    label: "ISLAND OF GUERNSEY",
    value: "ISLAND OF GUERNSEY",
  },
  {
    label: "MAURITIUS-MIG",
    value: "MAURITIAN-MIG",
  },
  {
    label: "JAMAICA",
    value: "JAMAICAN",
  },
  {
    label: "JORDAN",
    value: "JORDANIAN",
  },
  {
    label: "KAZAKHSTAN",
    value: "KAZAKH",
  },
  {
    label: "LAO DEMOCRATIC REPUBLIC",
    value: "LAO",
  },
  {
    label: "LIBYA",
    value: "LIBYAN",
  },
  {
    label: "LIECHTENSTEIN",
    value: "LIECHTENSTEINER",
  },
  {
    label: "LUXEMBOURG",
    value: "LUXEMBURG",
  },
  {
    label: "MALI",
    value: "MALIAN",
  },
  {
    label: "MALTA",
    value: "MALTESE",
  },
  {
    label: "KOSOVO",
    value: "KOSOVAN",
  },
  {
    label: "SAHARA",
    value: "SAHARA DEMO REP",
  },
  {
    label: "SIBERIA",
    value: "SIBERIAN",
  },
  {
    label: "MONROVIA",
    value: "MONROVIAN",
  },
  {
    label: "INTERPOL",
    value: "INTERPOL REPRESENTATIVE",
  },
  {
    label: "MONTENEGRO MNE",
    value: "MONTENEGRO MNE",
  },
  {
    label: "UNITED  NATIONS",
    value: "UN REPRESENTATIVE(UNA)",
  },
  {
    label: "ISLAND OF JERSEY",
    value: "ISLAND OF JERSEY",
  },
  {
    label: "DEMOCRATIC REP. OF CONGO",
    value: "CONGOLESE (DRC)",
  },
  {
    label: "AFRICAN REINSURANCE SORPO",
    value: "AFRICAN INSURANCE",
  },
  {
    label: "REPUBLIC OF SOMALILAND",
    value: "SOMALILANDER",
  },
  {
    label: "ISLE OF MAN",
    value: "MANN",
  },
  {
    label: "SOUTH SUDAN",
    value: "SOUTH SUDANESE",
  },
  {
    label: "ZIMBABWE ZIM",
    value: "ZIMBABWEAN ZIM",
  },
  {
    label: "SERBIA SRB",
    value: "SERBIAN SRB",
  },
  {
    label: "ROMANIA ROU",
    value: "ROMANIAN ROU",
  },
  {
    label: "UNITED NATIONS",
    value: "UN REPRESENTATIVE (UNO)",
  },
  {
    label: "MIG",
    value: "MIG",
  },
  {
    label: "FEDERAL STATE MICRONESIA",
    value: "MICRONESIAN",
  },
  {
    label: "MONACO",
    value: "MONACO",
  },
  {
    label: "MONGOLIA",
    value: "MONGOLIAN",
  },
  {
    label: "MONTSERRAT",
    value: "MONTSERRAT",
  },
  {
    label: "MOROCCO",
    value: "MOROCCAN",
  },
  {
    label: "MOZAMBIQUE",
    value: "MOZAMBIQUE",
  },
  {
    label: "MYANMAR",
    value: "MYANMAR",
  },
  {
    label: "NAMIBIA",
    value: "NAMIBIAN",
  },
  {
    label: "NAURU",
    value: "NAURU",
  },
  {
    label: "NEPAL",
    value: "NEPALESE",
  },
  {
    label: "NETHERLAND",
    value: "DUTCH",
  },
  {
    label: "NETHERLANDS ANTILLES",
    value: "NETHERLANDS ANTILLES",
  },
  {
    label: "NEUTRAL ZONE",
    value: "NEUTRAL ZONE",
  },
  {
    label: "NEW CALEDONIA",
    value: "NEW CALEDONIAN",
  },
  {
    label: "NEW ZEALAND",
    value: "NEW ZEALANDER",
  },
  {
    label: "NICARAGUA",
    value: "NICARAGUAN",
  },
  {
    label: "NIGER",
    value: "NIGERIEN",
  },
  {
    label: "NIGERIA",
    value: "NIGERIAN",
  },
  {
    label: "NIUE",
    value: "NIUE",
  },
  {
    label: "NORFOLK ISLAND",
    value: "NORFOLK ISLAND",
  },
  {
    label: "NOTHERN MARIANA ISLANDS",
    value: "NOTHERN MARIANA ISLANDS",
  },
  {
    label: "NORWAY",
    value: "NORWEGIAN",
  },
  {
    label: "OMAN",
    value: "OMAN",
  },
  {
    label: "GERMANY",
    value: "GERMAN",
  },
  {
    label: "ITALY",
    value: "ITALIAN",
  },
  {
    label: "PORTUGAL",
    value: "PORTUGUESE",
  },
  {
    label: "SLOVENIA ",
    value: "SLOVENE",
  },
  {
    label: "PALASTINE",
    value: "PALASTINIAN",
  },
  {
    label: "MIANMAR",
    value: "MIANMARIAN",
  },
  {
    label: "YUGOSLOVIA",
    value: "YUGOSLOVIAN",
  },
  {
    label: "TRINIDAD AND TOBAGO",
    value: "TRINIDADIAN",
  },
  {
    label: "SERBIA SCG",
    value: "SERB SCG",
  },
  {
    label: "SURINAME",
    value: "SURINAMIAN",
  },
  {
    label: "SYRIAN ARAB REPUBLIC",
    value: "SYRIAN",
  },
  {
    label: "EAST TIMOR",
    value: "EAST TIMORESE",
  },
  {
    label: "REPUBLIC OF TANZANIA",
    value: "TANZANIAN",
  },
  {
    label: "VENEZUELA",
    value: "VENEZUELAN",
  },
  {
    label: "ARGENTINA",
    value: "ARGENTINE",
  },
  {
    label: "CENTRAL AFRICAN REPUBLIC",
    value: "CENTRAL AFRICAN REPUBLIC",
  },
  {
    label: "ECUADOR",
    value: "ECUDORIAN",
  },
  {
    label: "UK - PROTECTED",
    value: "BRITISH - PROTECTED",
  },
  {
    label: "PANAMA",
    value: "PANAMANIAN",
  },
  {
    label: "ROMANIA ROM",
    value: "ROMANIAN ROM",
  },
  {
    label: "SIERRA LEONE",
    value: "SIERRA LEONEAN",
  },
  {
    label: "TURKMENISTAN",
    value: "TURKMENISTANI",
  },
  {
    label: "IVORY COAST",
    value: "IVOIRIAN",
  },
  {
    label: "TONGA",
    value: "TONGAN",
  },
  {
    label: "CAYMAN ISLANDS",
    value: "CAYMEN ISLANDS",
  },
  {
    label: "ANGUILLA",
    value: "ANGUILLAN",
  },
  {
    label: "COOK ISLANDS",
    value: "COOK ISLANDS",
  },
  {
    label: "REUNION",
    value: "REUNIONNAIS",
  },
  {
    label: "AUSTRALIA",
    value: "AUSTRALIAN",
  },
  {
    label: "BANGLADESH",
    value: "BANGLADESHI",
  },
  {
    label: "BRAZIL",
    value: "BRAZILIAN",
  },
  {
    label: "CANADA",
    value: "CANADIAN",
  },
  {
    label: "SRI LANKA",
    value: "SRI LANKAN",
  },
  {
    label: "INDIA",
    value: "INDIAN",
  },
  {
    label: "HONG KONG",
    value: "HONG KONG",
  },
  {
    label: "AFGHANISTAN",
    value: "AFGHAN",
  },
  {
    label: "ALBANIA",
    value: "ALBANIAN",
  },
  {
    label: "ALGERIA",
    value: "ALGERIAN",
  },
  {
    label: "AMERICAN SAMOA",
    value: "AMERICAN SAMOAN",
  },
  {
    label: "ANDORRA",
    value: "ANDORRAN",
  },
  {
    label: "ANGOLA",
    value: "ANGOLAN",
  },
  {
    label: "ANTARCTICA",
    value: "ANTARCTICAN",
  },
  {
    label: "ANTIGUA AND BARBUDA",
    value: "ANTIGUAN",
  },
  {
    label: "ARMENIA",
    value: "ARMENIAN",
  },
  {
    label: "ARUBA",
    value: "ARUBAN",
  },
  {
    label: "AUSTRIA",
    value: "AUSTRIAN",
  },
  {
    label: "AZERBAIJAN",
    value: "AZERI",
  },
  {
    label: "BAHAMAS",
    value: "BAHAMIAN",
  },
  {
    label: "BAHRAIN",
    value: "BAHRAINI",
  },
  {
    label: "BARBADOS",
    value: "BARBADIAN",
  },
  {
    label: "BELARUS",
    value: "BELARUSIAN",
  },
  {
    label: "BELGIUM",
    value: "BELGIAN",
  },
  {
    label: "BELIZE",
    value: "BELIZEAN",
  },
  {
    label: "BENIN",
    value: "BENIN",
  },
  {
    label: "BERMUDA",
    value: "BERMUDIAN",
  },
  {
    label: "BHUTAN",
    value: "BHUTANIAN",
  },
  {
    label: "BOLIVIA",
    value: "BOLIVIAN",
  },
  {
    label: "BOZNIA HERZEGOVNIA",
    value: "BOSNIAN",
  },
  {
    label: "BOTSWANA",
    value: "BOTSWANIAN",
  },
  {
    label: "BOUVET ISLAND",
    value: "BOUVET ISLAND",
  },
  {
    label: "BRITISH INDIAN OCEAN",
    value: "BRITISH INDIAN OCEAN",
  },
  {
    label: "BRUNEI DARUSSALAM",
    value: "BRUNIE DARUSSALAM",
  },
  {
    label: "BULGARIA",
    value: "BULGARIAN",
  },
  {
    label: "BURKINA FASO",
    value: "BURKINABE",
  },
  {
    label: "BURUNDI",
    value: "BURUNDIAN",
  },
  {
    label: "CAMBODIA",
    value: "CAMBODIAN",
  },
  {
    label: "CAMEROON",
    value: "CAMEROONIAN",
  },
  {
    label: "CAPE VERDE",
    value: "CAPE VERDEAN",
  },
  {
    label: "CHAD",
    value: "CHADIAN",
  },
  {
    label: "CHILE",
    value: "CHILEAN",
  },
  {
    label: "CHINA",
    value: "CHINESE",
  },
  {
    label: "CHRISTMAS ISLANDS",
    value: "CHRISTMAS ISLANDS",
  },
  {
    label: "COCOS (KEELING) ISLANDS",
    value: "COCOS (KEELING) ISLANDS",
  },
  {
    label: "COLOMBIA",
    value: "COLOMBIAN",
  },
  {
    label: "COMOROS",
    value: "COMORIAN",
  },
  {
    label: "MONTENEGRO MON",
    value: "MONTENEGRO MON",
  },
];

const valuesList = [];

for (var nationality in nationalityDetails) {
  var nationalityItem = nationalityDetails[nationality];
  valuesList.push({
    value: nationalityItem.value,
    label: nationalityItem.value,
  });
}

export const nationalityList = valuesList.sort((a, b) =>
  a.label.localeCompare(b.label),
);
