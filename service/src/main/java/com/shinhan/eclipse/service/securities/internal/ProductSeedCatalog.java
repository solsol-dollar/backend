package com.shinhan.eclipse.service.securities.internal;

import java.util.List;

final class ProductSeedCatalog {

    static final String OVERSEAS = "OVERSEAS";
    static final String ETF      = "ETF";
    static final String NASDAQ   = "NASDAQ";
    static final String NYSE     = "NYSE Arca";

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

    /** 인기 해외 ETF */
    static final List<Seed> ETF_LIST = List.of(
        new Seed("QQQ",  "Invesco QQQ Trust",                      "NASDAQ 100 Index",   ETF, NASDAQ),
        new Seed("SOXX", "iShares Semiconductor ETF",              "Semiconductors",     ETF, NYSE),
        new Seed("ARKK", "ARK Innovation ETF",                     "Growth/Innovation",  ETF, NYSE),
        new Seed("SPY",  "SPDR S&P 500 ETF Trust",                 "S&P 500 Index",      ETF, NYSE),
        new Seed("VOO",  "Vanguard S&P 500 ETF",                   "S&P 500 Index",      ETF, NYSE),
        new Seed("VTI",  "Vanguard Total Stock Market ETF",        "Total Market",       ETF, NYSE),
        new Seed("XLK",  "Technology Select Sector SPDR Fund",     "Technology",         ETF, NYSE),
        new Seed("SCHD", "Schwab US Dividend Equity ETF",          "Dividend",           ETF, NYSE),
        new Seed("GDX",  "VanEck Gold Miners ETF",                 "Gold Miners",        ETF, NYSE),
        new Seed("IAU",  "iShares Gold Trust",                     "Gold",               ETF, NYSE)
    );

    private ProductSeedCatalog() {}
}
