package com.shinhan.eclipse.service.securities.internal;

import java.util.List;

final class ProductSeedCatalog {

    static final String OVERSEAS   = "OVERSEAS";
    static final String ETF        = "ETF";
    static final String NASDAQ     = "NASDAQ";
    static final String NYSE       = "NYSE";        // S&P 500 주식용
    static final String NYSE_ARCA  = "NYSE Arca";   // ETF용

    record Seed(String ticker, String productName, String sector, String productType, String exchangeName) {}

    /** NASDAQ 100 구성 종목 (2026년 기준 근사치) */
    static final List<Seed> NASDAQ_100 = List.of(
        // Technology
        new Seed("AAPL",  "Apple Inc",                       "Technology",              OVERSEAS, NASDAQ),
        new Seed("MSFT",  "Microsoft Corp",                  "Technology",              OVERSEAS, NASDAQ),
        new Seed("NVDA",  "NVIDIA Corp",                     "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("META",  "Meta Platforms Inc",              "Communication Services",  OVERSEAS, NASDAQ),
        new Seed("GOOGL", "Alphabet Inc Class A",            "Communication Services",  OVERSEAS, NASDAQ),
        new Seed("GOOG",  "Alphabet Inc Class C",            "Communication Services",  OVERSEAS, NASDAQ),
        new Seed("ADBE",  "Adobe Inc",                       "Technology",              OVERSEAS, NASDAQ),
        new Seed("CSCO",  "Cisco Systems Inc",               "Technology",              OVERSEAS, NASDAQ),
        new Seed("INTC",  "Intel Corp",                      "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("TXN",   "Texas Instruments Inc",           "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("INTU",  "Intuit Inc",                      "Technology",              OVERSEAS, NASDAQ),
        new Seed("QCOM",  "Qualcomm Inc",                    "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("AMAT",  "Applied Materials Inc",           "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("ADP",   "Automatic Data Processing Inc",   "Technology",              OVERSEAS, NASDAQ),
        new Seed("ADI",   "Analog Devices Inc",              "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("MU",    "Micron Technology Inc",           "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("KLAC",  "KLA Corp",                        "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("SNPS",  "Synopsys Inc",                    "Technology",              OVERSEAS, NASDAQ),
        new Seed("CDNS",  "Cadence Design Systems Inc",      "Technology",              OVERSEAS, NASDAQ),
        new Seed("PANW",  "Palo Alto Networks Inc",          "Technology",              OVERSEAS, NASDAQ),
        new Seed("FTNT",  "Fortinet Inc",                    "Technology",              OVERSEAS, NASDAQ),
        new Seed("CRWD",  "CrowdStrike Holdings Inc",        "Technology",              OVERSEAS, NASDAQ),
        new Seed("MRVL",  "Marvell Technology Inc",          "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("ASML",  "ASML Holding NV",                 "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("LRCX",  "Lam Research Corp",               "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("NXPI",  "NXP Semiconductors NV",           "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("ON",    "ON Semiconductor Corp",           "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("MCHP",  "Microchip Technology Inc",        "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("TEAM",  "Atlassian Corp",                  "Technology",              OVERSEAS, NASDAQ),
        new Seed("WDAY",  "Workday Inc",                     "Technology",              OVERSEAS, NASDAQ),
        new Seed("DDOG",  "Datadog Inc",                     "Technology",              OVERSEAS, NASDAQ),
        new Seed("ZS",    "Zscaler Inc",                     "Technology",              OVERSEAS, NASDAQ),
        new Seed("NET",   "Cloudflare Inc",                  "Technology",              OVERSEAS, NASDAQ),
        new Seed("ANSS",  "ANSYS Inc",                       "Technology",              OVERSEAS, NASDAQ),
        new Seed("VRSK",  "Verisk Analytics Inc",            "Technology",              OVERSEAS, NASDAQ),
        new Seed("CDW",   "CDW Corp",                        "Technology",              OVERSEAS, NASDAQ),
        new Seed("CSGP",  "CoStar Group Inc",                "Technology",              OVERSEAS, NASDAQ),
        new Seed("PYPL",  "PayPal Holdings Inc",             "Technology",              OVERSEAS, NASDAQ),
        new Seed("CTSH",  "Cognizant Technology Solutions",  "Technology",              OVERSEAS, NASDAQ),
        new Seed("EBAY",  "eBay Inc",                        "Consumer Discretionary",  OVERSEAS, NASDAQ),

        // Consumer Discretionary
        new Seed("AMZN",  "Amazon.com Inc",                  "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("TSLA",  "Tesla Inc",                       "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("BKNG",  "Booking Holdings Inc",            "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("SBUX",  "Starbucks Corp",                  "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("MELI",  "MercadoLibre Inc",                "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("ORLY",  "O'Reilly Automotive Inc",         "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("ABNB",  "Airbnb Inc",                      "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("ROST",  "Ross Stores Inc",                 "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("DLTR",  "Dollar Tree Inc",                 "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("MAR",   "Marriott International Inc",      "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("LULU",  "Lululemon Athletica Inc",         "Consumer Discretionary",  OVERSEAS, NASDAQ),
        new Seed("AMD",   "Advanced Micro Devices Inc",      "Semiconductors",          OVERSEAS, NASDAQ),

        // Communication Services
        new Seed("NFLX",  "Netflix Inc",                     "Communication Services",  OVERSEAS, NASDAQ),
        new Seed("TMUS",  "T-Mobile US Inc",                 "Communication Services",  OVERSEAS, NASDAQ),
        new Seed("CHTR",  "Charter Communications Inc",      "Communication Services",  OVERSEAS, NASDAQ),
        new Seed("SIRI",  "Sirius XM Holdings Inc",          "Communication Services",  OVERSEAS, NASDAQ),
        new Seed("WBD",   "Warner Bros Discovery Inc",       "Communication Services",  OVERSEAS, NASDAQ),
        new Seed("EA",    "Electronic Arts Inc",             "Communication Services",  OVERSEAS, NASDAQ),
        new Seed("TTWO",  "Take-Two Interactive Software",   "Communication Services",  OVERSEAS, NASDAQ),

        // Healthcare & Biotech
        new Seed("AMGN",  "Amgen Inc",                       "Biotechnology",           OVERSEAS, NASDAQ),
        new Seed("GILD",  "Gilead Sciences Inc",             "Biotechnology",           OVERSEAS, NASDAQ),
        new Seed("ISRG",  "Intuitive Surgical Inc",          "Healthcare",              OVERSEAS, NASDAQ),
        new Seed("REGN",  "Regeneron Pharmaceuticals Inc",   "Biotechnology",           OVERSEAS, NASDAQ),
        new Seed("VRTX",  "Vertex Pharmaceuticals Inc",      "Biotechnology",           OVERSEAS, NASDAQ),
        new Seed("IDXX",  "IDEXX Laboratories Inc",          "Healthcare",              OVERSEAS, NASDAQ),
        new Seed("BIIB",  "Biogen Inc",                      "Biotechnology",           OVERSEAS, NASDAQ),
        new Seed("DXCM",  "DexCom Inc",                      "Healthcare",              OVERSEAS, NASDAQ),
        new Seed("ILMN",  "Illumina Inc",                    "Healthcare",              OVERSEAS, NASDAQ),
        new Seed("ALGN",  "Align Technology Inc",            "Healthcare",              OVERSEAS, NASDAQ),
        new Seed("GEHC",  "GE HealthCare Technologies",      "Healthcare",              OVERSEAS, NASDAQ),

        // Consumer Staples
        new Seed("PEP",   "PepsiCo Inc",                     "Consumer Staples",        OVERSEAS, NASDAQ),
        new Seed("COST",  "Costco Wholesale Corp",           "Consumer Staples",        OVERSEAS, NASDAQ),
        new Seed("MDLZ",  "Mondelez International Inc",      "Consumer Staples",        OVERSEAS, NASDAQ),
        new Seed("KDP",   "Keurig Dr Pepper Inc",            "Consumer Staples",        OVERSEAS, NASDAQ),
        new Seed("MNST",  "Monster Beverage Corp",           "Consumer Staples",        OVERSEAS, NASDAQ),
        new Seed("WBA",   "Walgreens Boots Alliance Inc",    "Consumer Staples",        OVERSEAS, NASDAQ),
        new Seed("KHC",   "Kraft Heinz Co",                  "Consumer Staples",        OVERSEAS, NASDAQ),

        // Industrials
        new Seed("HON",   "Honeywell International Inc",     "Industrials",             OVERSEAS, NASDAQ),
        new Seed("PCAR",  "PACCAR Inc",                      "Industrials",             OVERSEAS, NASDAQ),
        new Seed("FAST",  "Fastenal Co",                     "Industrials",             OVERSEAS, NASDAQ),
        new Seed("ODFL",  "Old Dominion Freight Line Inc",   "Industrials",             OVERSEAS, NASDAQ),
        new Seed("CTAS",  "Cintas Corp",                     "Industrials",             OVERSEAS, NASDAQ),
        new Seed("PAYX",  "Paychex Inc",                     "Industrials",             OVERSEAS, NASDAQ),
        new Seed("CPRT",  "Copart Inc",                      "Industrials",             OVERSEAS, NASDAQ),

        // Utilities
        new Seed("EXC",   "Exelon Corp",                     "Utilities",               OVERSEAS, NASDAQ),
        new Seed("AEP",   "American Electric Power Co",      "Utilities",               OVERSEAS, NASDAQ),
        new Seed("XEL",   "Xcel Energy Inc",                 "Utilities",               OVERSEAS, NASDAQ),
        new Seed("CEG",   "Constellation Energy Corp",       "Utilities",               OVERSEAS, NASDAQ),

        // Energy
        new Seed("FANG",  "Diamondback Energy Inc",          "Energy",                  OVERSEAS, NASDAQ),

        // Finance / Crypto
        new Seed("COIN",  "Coinbase Global Inc",             "Financial Services",      OVERSEAS, NASDAQ),
        new Seed("HOOD",  "Robinhood Markets Inc",           "Financial Services",      OVERSEAS, NASDAQ),

        // Other
        new Seed("AVGO",  "Broadcom Inc",                    "Semiconductors",          OVERSEAS, NASDAQ),
        new Seed("ROKU",  "Roku Inc",                        "Technology",              OVERSEAS, NASDAQ),
        new Seed("ZM",    "Zoom Video Communications",       "Technology",              OVERSEAS, NASDAQ),
        new Seed("DOCU",  "DocuSign Inc",                    "Technology",              OVERSEAS, NASDAQ),
        new Seed("OKTA",  "Okta Inc",                        "Technology",              OVERSEAS, NASDAQ),
        new Seed("GFS",   "GlobalFoundries Inc",             "Semiconductors",          OVERSEAS, NASDAQ)
    );

    /** S&P 500 구성 종목 중 NASDAQ 100에 없는 NYSE 상장 종목 */
    static final List<Seed> SP500_NYSE = List.of(
        // Financials
        new Seed("JPM",   "JPMorgan Chase & Co",           "Financials",             OVERSEAS, NYSE),
        new Seed("BAC",   "Bank of America Corp",           "Financials",             OVERSEAS, NYSE),
        new Seed("WFC",   "Wells Fargo & Co",               "Financials",             OVERSEAS, NYSE),
        new Seed("C",     "Citigroup Inc",                  "Financials",             OVERSEAS, NYSE),
        new Seed("GS",    "Goldman Sachs Group Inc",        "Financials",             OVERSEAS, NYSE),
        new Seed("MS",    "Morgan Stanley",                 "Financials",             OVERSEAS, NYSE),
        new Seed("BLK",   "BlackRock Inc",                  "Financials",             OVERSEAS, NYSE),
        new Seed("AXP",   "American Express Co",            "Financials",             OVERSEAS, NYSE),
        new Seed("V",     "Visa Inc",                       "Financials",             OVERSEAS, NYSE),
        new Seed("MA",    "Mastercard Inc",                 "Financials",             OVERSEAS, NYSE),
        new Seed("SCHW",  "Charles Schwab Corp",            "Financials",             OVERSEAS, NYSE),
        new Seed("BK",    "Bank of New York Mellon Corp",   "Financials",             OVERSEAS, NYSE),
        new Seed("USB",   "US Bancorp",                     "Financials",             OVERSEAS, NYSE),
        new Seed("PNC",   "PNC Financial Services Group",   "Financials",             OVERSEAS, NYSE),
        new Seed("CB",    "Chubb Ltd",                      "Insurance",              OVERSEAS, NYSE),
        new Seed("MMC",   "Marsh & McLennan Companies",     "Insurance",              OVERSEAS, NYSE),
        new Seed("TRV",   "Travelers Companies Inc",        "Insurance",              OVERSEAS, NYSE),
        new Seed("AFL",   "Aflac Inc",                      "Insurance",              OVERSEAS, NYSE),
        new Seed("MET",   "MetLife Inc",                    "Insurance",              OVERSEAS, NYSE),
        new Seed("PRU",   "Prudential Financial Inc",       "Insurance",              OVERSEAS, NYSE),

        // Healthcare
        new Seed("JNJ",   "Johnson & Johnson",              "Healthcare",             OVERSEAS, NYSE),
        new Seed("UNH",   "UnitedHealth Group Inc",         "Healthcare",             OVERSEAS, NYSE),
        new Seed("LLY",   "Eli Lilly and Co",               "Healthcare",             OVERSEAS, NYSE),
        new Seed("ABBV",  "AbbVie Inc",                     "Healthcare",             OVERSEAS, NYSE),
        new Seed("MRK",   "Merck & Co Inc",                 "Healthcare",             OVERSEAS, NYSE),
        new Seed("PFE",   "Pfizer Inc",                     "Healthcare",             OVERSEAS, NYSE),
        new Seed("TMO",   "Thermo Fisher Scientific Inc",   "Healthcare",             OVERSEAS, NYSE),
        new Seed("ABT",   "Abbott Laboratories",            "Healthcare",             OVERSEAS, NYSE),
        new Seed("DHR",   "Danaher Corp",                   "Healthcare",             OVERSEAS, NYSE),
        new Seed("BMY",   "Bristol-Myers Squibb Co",        "Healthcare",             OVERSEAS, NYSE),
        new Seed("ELV",   "Elevance Health Inc",            "Healthcare",             OVERSEAS, NYSE),
        new Seed("SYK",   "Stryker Corp",                   "Healthcare",             OVERSEAS, NYSE),
        new Seed("CVS",   "CVS Health Corp",                "Healthcare",             OVERSEAS, NYSE),
        new Seed("MDT",   "Medtronic plc",                  "Healthcare",             OVERSEAS, NYSE),
        new Seed("BSX",   "Boston Scientific Corp",         "Healthcare",             OVERSEAS, NYSE),
        new Seed("CI",    "Cigna Group",                    "Healthcare",             OVERSEAS, NYSE),

        // Consumer Staples
        new Seed("WMT",   "Walmart Inc",                    "Consumer Staples",       OVERSEAS, NYSE),
        new Seed("PG",    "Procter & Gamble Co",            "Consumer Staples",       OVERSEAS, NYSE),
        new Seed("KO",    "Coca-Cola Co",                   "Consumer Staples",       OVERSEAS, NYSE),
        new Seed("PM",    "Philip Morris International",    "Consumer Staples",       OVERSEAS, NYSE),
        new Seed("MO",    "Altria Group Inc",               "Consumer Staples",       OVERSEAS, NYSE),
        new Seed("CL",    "Colgate-Palmolive Co",           "Consumer Staples",       OVERSEAS, NYSE),
        new Seed("GIS",   "General Mills Inc",              "Consumer Staples",       OVERSEAS, NYSE),
        new Seed("K",     "Kellanova",                      "Consumer Staples",       OVERSEAS, NYSE),
        new Seed("HSY",   "Hershey Co",                     "Consumer Staples",       OVERSEAS, NYSE),

        // Consumer Discretionary
        new Seed("MCD",   "McDonald's Corp",                "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("NKE",   "Nike Inc",                       "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("LOW",   "Lowe's Companies Inc",           "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("DIS",   "Walt Disney Co",                 "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("GM",    "General Motors Co",              "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("F",     "Ford Motor Co",                  "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("TGT",   "Target Corp",                    "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("HLT",   "Hilton Worldwide Holdings Inc",  "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("YUM",   "Yum Brands Inc",                 "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("RCL",   "Royal Caribbean Cruises Ltd",    "Consumer Discretionary", OVERSEAS, NYSE),
        new Seed("CCL",   "Carnival Corp",                  "Consumer Discretionary", OVERSEAS, NYSE),

        // Energy
        new Seed("XOM",   "Exxon Mobil Corp",               "Energy",                 OVERSEAS, NYSE),
        new Seed("CVX",   "Chevron Corp",                   "Energy",                 OVERSEAS, NYSE),
        new Seed("COP",   "ConocoPhillips",                 "Energy",                 OVERSEAS, NYSE),
        new Seed("SLB",   "SLB",                            "Energy",                 OVERSEAS, NYSE),
        new Seed("EOG",   "EOG Resources Inc",              "Energy",                 OVERSEAS, NYSE),
        new Seed("PSX",   "Phillips 66",                    "Energy",                 OVERSEAS, NYSE),
        new Seed("VLO",   "Valero Energy Corp",             "Energy",                 OVERSEAS, NYSE),
        new Seed("OXY",   "Occidental Petroleum Corp",      "Energy",                 OVERSEAS, NYSE),

        // Industrials
        new Seed("CAT",   "Caterpillar Inc",                "Industrials",            OVERSEAS, NYSE),
        new Seed("GE",    "GE Aerospace",                   "Industrials",            OVERSEAS, NYSE),
        new Seed("RTX",   "RTX Corp",                       "Industrials",            OVERSEAS, NYSE),
        new Seed("UPS",   "United Parcel Service Inc",      "Industrials",            OVERSEAS, NYSE),
        new Seed("BA",    "Boeing Co",                      "Industrials",            OVERSEAS, NYSE),
        new Seed("DE",    "Deere & Co",                     "Industrials",            OVERSEAS, NYSE),
        new Seed("MMM",   "3M Co",                          "Industrials",            OVERSEAS, NYSE),
        new Seed("EMR",   "Emerson Electric Co",            "Industrials",            OVERSEAS, NYSE),
        new Seed("ETN",   "Eaton Corp plc",                 "Industrials",            OVERSEAS, NYSE),
        new Seed("ITW",   "Illinois Tool Works Inc",        "Industrials",            OVERSEAS, NYSE),
        new Seed("LMT",   "Lockheed Martin Corp",           "Industrials",            OVERSEAS, NYSE),
        new Seed("NOC",   "Northrop Grumman Corp",          "Industrials",            OVERSEAS, NYSE),
        new Seed("GD",    "General Dynamics Corp",          "Industrials",            OVERSEAS, NYSE),
        new Seed("FDX",   "FedEx Corp",                     "Industrials",            OVERSEAS, NYSE),

        // Technology (NYSE-listed)
        new Seed("IBM",   "International Business Machines","Technology",             OVERSEAS, NYSE),
        new Seed("ACN",   "Accenture plc",                  "Technology",             OVERSEAS, NYSE),
        new Seed("CRM",   "Salesforce Inc",                 "Technology",             OVERSEAS, NYSE),
        new Seed("NOW",   "ServiceNow Inc",                 "Technology",             OVERSEAS, NYSE),
        new Seed("ORCL",  "Oracle Corp",                    "Technology",             OVERSEAS, NYSE),

        // Communication (NYSE)
        new Seed("T",     "AT&T Inc",                       "Communication Services", OVERSEAS, NYSE),
        new Seed("VZ",    "Verizon Communications Inc",     "Communication Services", OVERSEAS, NYSE),
        new Seed("CMCSA", "Comcast Corp",                   "Communication Services", OVERSEAS, NYSE),

        // Materials
        new Seed("LIN",   "Linde plc",                      "Materials",              OVERSEAS, NYSE),
        new Seed("APD",   "Air Products & Chemicals Inc",   "Materials",              OVERSEAS, NYSE),
        new Seed("SHW",   "Sherwin-Williams Co",            "Materials",              OVERSEAS, NYSE),
        new Seed("FCX",   "Freeport-McMoRan Inc",           "Materials",              OVERSEAS, NYSE),
        new Seed("NEM",   "Newmont Corp",                   "Materials",              OVERSEAS, NYSE),
        new Seed("ECL",   "Ecolab Inc",                     "Materials",              OVERSEAS, NYSE),
        new Seed("DD",    "DuPont de Nemours Inc",          "Materials",              OVERSEAS, NYSE),

        // Utilities
        new Seed("NEE",   "NextEra Energy Inc",             "Utilities",              OVERSEAS, NYSE),
        new Seed("DUK",   "Duke Energy Corp",               "Utilities",              OVERSEAS, NYSE),
        new Seed("SO",    "Southern Co",                    "Utilities",              OVERSEAS, NYSE),
        new Seed("D",     "Dominion Energy Inc",            "Utilities",              OVERSEAS, NYSE),
        new Seed("SRE",   "Sempra",                         "Utilities",              OVERSEAS, NYSE),
        new Seed("PCG",   "PG&E Corp",                      "Utilities",              OVERSEAS, NYSE),

        // Real Estate
        new Seed("AMT",   "American Tower Corp",            "Real Estate",            OVERSEAS, NYSE),
        new Seed("PLD",   "Prologis Inc",                   "Real Estate",            OVERSEAS, NYSE),
        new Seed("CCI",   "Crown Castle Inc",               "Real Estate",            OVERSEAS, NYSE),
        new Seed("SPG",   "Simon Property Group Inc",       "Real Estate",            OVERSEAS, NYSE),
        new Seed("O",     "Realty Income Corp",             "Real Estate",            OVERSEAS, NYSE),
        new Seed("WELL",  "Welltower Inc",                  "Real Estate",            OVERSEAS, NYSE)
    );

    /** 인기 해외 ETF */
    static final List<Seed> ETF_LIST = List.of(
        // 브로드마켓 / 인덱스
        new Seed("QQQ",  "Invesco QQQ Trust",                                    "NASDAQ 100 Index",        ETF, NASDAQ),
        new Seed("SOXX", "iShares Semiconductor ETF",                            "Semiconductors",          ETF, NYSE_ARCA),
        new Seed("ARKK", "ARK Innovation ETF",                                   "Growth/Innovation",       ETF, NYSE_ARCA),
        new Seed("SPY",  "SPDR S&P 500 ETF Trust",                               "S&P 500 Index",           ETF, NYSE_ARCA),
        new Seed("VOO",  "Vanguard S&P 500 ETF",                                 "S&P 500 Index",           ETF, NYSE_ARCA),
        new Seed("VTI",  "Vanguard Total Stock Market ETF",                      "Total Market",            ETF, NYSE_ARCA),
        new Seed("XLK",  "Technology Select Sector SPDR Fund",                   "Technology",              ETF, NYSE_ARCA),
        new Seed("SCHD", "Schwab US Dividend Equity ETF",                        "Dividend",                ETF, NYSE_ARCA),
        new Seed("GDX",  "VanEck Gold Miners ETF",                               "Gold Miners",             ETF, NYSE_ARCA),
        new Seed("IAU",  "iShares Gold Trust",                                   "Gold",                    ETF, NYSE_ARCA),
        // IT / Tech
        new Seed("IGV",  "iShares Expanded Tech-Software Sector ETF",            "IT / Tech",               ETF, NASDAQ),
        new Seed("WCLD", "WisdomTree Cloud Computing Fund",                       "IT / Tech",               ETF, NASDAQ),
        new Seed("AIQ",  "Global X Artificial Intelligence & Tech ETF",           "IT / Tech",               ETF, NASDAQ),
        // Healthcare / Biotech
        new Seed("XBI",  "SPDR S&P Biotech ETF",                                 "Healthcare / Biotech",    ETF, NYSE_ARCA),
        new Seed("IBB",  "iShares Biotechnology ETF",                             "Healthcare / Biotech",    ETF, NASDAQ),
        new Seed("ARKG", "ARK Genomic Revolution ETF",                            "Healthcare / Biotech",    ETF, NYSE_ARCA),
        // Industrials / Defense
        new Seed("ITA",  "iShares U.S. Aerospace & Defense ETF",                  "Industrials / Defense",   ETF, NYSE_ARCA),
        new Seed("SHLD", "Global X Defense Tech ETF",                             "Industrials / Defense",   ETF, NASDAQ),
        new Seed("ARKX", "ARK Space Exploration & Innovation ETF",                "Industrials / Defense",   ETF, NYSE_ARCA),
        // Finance / FinTech
        new Seed("FINX", "Global X FinTech ETF",                                  "Finance / FinTech",       ETF, NASDAQ),
        new Seed("IPAY", "ETFMG Prime Mobile Payments ETF",                       "Finance / FinTech",       ETF, NYSE_ARCA),
        new Seed("BLOK", "Amplify Transformational Data Sharing ETF",             "Finance / FinTech",       ETF, NYSE_ARCA),
        // Energy / Utilities
        new Seed("ICLN", "iShares Global Clean Energy ETF",                       "Energy / Utilities",      ETF, NASDAQ),
        new Seed("CNRG", "SPDR S&P Kensho Clean Power ETF",                       "Energy / Utilities",      ETF, NYSE_ARCA),
        new Seed("AMLP", "Alerian MLP ETF",                                       "Energy / Utilities",      ETF, NYSE_ARCA),
        // Materials / Mining
        new Seed("XME",  "SPDR S&P Metals & Mining ETF",                          "Materials / Mining",      ETF, NYSE_ARCA),
        new Seed("COPX", "Global X Copper Miners ETF",                            "Materials / Mining",      ETF, NYSE_ARCA),
        new Seed("REMX", "VanEck Rare Earth & Strategic Metals ETF",              "Materials / Mining",      ETF, NYSE_ARCA),
        // Consumer Staples / Discretionary
        new Seed("PBJ",  "Invesco Dynamic Food & Beverage ETF",                   "Consumer Staples / Disc", ETF, NYSE_ARCA),
        new Seed("XRT",  "SPDR S&P Retail ETF",                                   "Consumer Staples / Disc", ETF, NYSE_ARCA),
        new Seed("FDIS", "Fidelity MSCI Consumer Discretionary ETF",              "Consumer Staples / Disc", ETF, NYSE_ARCA),
        // Real Estate / Infra
        new Seed("SRVR", "Pacer Data & Infrastructure Real Estate ETF",           "Real Estate / Infra",     ETF, NASDAQ),
        new Seed("IFRA", "iShares U.S. Infrastructure ETF",                       "Real Estate / Infra",     ETF, NYSE_ARCA),
        new Seed("PAVE", "Global X U.S. Infrastructure Development ETF",          "Real Estate / Infra",     ETF, NYSE_ARCA),
        // Logistics / Mobility
        new Seed("IYT",  "iShares U.S. Transportation ETF",                       "Logistics / Mobility",    ETF, NYSE_ARCA),
        new Seed("XTN",  "SPDR S&P Transportation ETF",                           "Logistics / Mobility",    ETF, NYSE_ARCA),
        new Seed("DRIV", "Global X Autonomous & Electric Vehicles ETF",           "Logistics / Mobility",    ETF, NASDAQ)
    );

    private ProductSeedCatalog() {}
}
