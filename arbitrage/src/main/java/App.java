package main.java;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.binance.BinanceExchange;
import org.knowm.xchange.bitfinex.v1.BitfinexExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.poloniex.PoloniexExchange;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.params.TradeHistoryParamsAll;



public class App {
	
	public static List<String> exchangesNames = null;
	public static List<CurrencyPair> tradable_pairs = null;
	public static Map<String,Exchange> exchanges = null;
	
	
	public static void connectExchanges() {
		for ( String s: exchangesNames)
		{
			ExchangeSpecification spec = null;			
			switch (s)
			{
			case "bitfinex":
				spec = new BitfinexExchange().getDefaultExchangeSpecification();
				break;
			case "binance":
				spec = new BinanceExchange().getDefaultExchangeSpecification();
				break;
			case "poloniex":
				spec = new PoloniexExchange().getDefaultExchangeSpecification();
				break;
			}
			
			spec.setApiKey(System.getenv(s+"_APIKEY"));
			spec.setSecretKey(System.getenv(s+"_SECRET"));
			exchanges.put(s, ExchangeFactory.INSTANCE.createExchange(spec));			
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
		tradable_pairs = new Vector<CurrencyPair>();

		
		exchangesNames  =  new Vector<String>();
		exchangesNames.add("bitfinex");
		exchangesNames.add("binance");
		exchangesNames.add("poloniex");
		
		exchanges = new HashMap<String,Exchange>();
		
		connectExchanges();
		
		
		Map<String,List<CurrencyPair>> all_eth_symbols = new HashMap<String,List<CurrencyPair>>();
		
		for ( String key: exchangesNames )
		{
			List<CurrencyPair> eth_symbols = new Vector<CurrencyPair>();
			List<CurrencyPair> symbols = exchanges.get(key).getExchangeSymbols();
			
			for (CurrencyPair cp : symbols)
			{
				if ( cp.counter == Currency.ETH )
				{
					//System.out.println(cp);				
					eth_symbols.add(cp);
				}
			}
			//System.out.println();
			all_eth_symbols.put(key, eth_symbols);
		}
		
		
		System.out.println();
		
		for ( String key: exchangesNames ) 
		{
			System.out.println(key+" : ");
			for ( String key2: exchangesNames )
			{
				if ( key != key2 )
				{
					System.out.println("\t"+key2);
					System.out.print("\t");
					for ( CurrencyPair elem: all_eth_symbols.get(key) )
					{
						for ( CurrencyPair elem2: all_eth_symbols.get(key2) )
						{
							if ( elem.compareTo(elem2) == 0 )
							{
								System.out.print(elem.toString()+" ");
								break;
							}
						}
					}
					System.out.println();					
				}
			}

		}

		
		for ( String key: exchangesNames )
		{
			MarketDataService marketDataService = exchanges.get(key).getMarketDataService();

				//System.out.println(exchanges.get(key).getAccountService().requestDepositAddress(Currency.EOS).toString());
				TradeHistoryParamsAll thp = new TradeHistoryParamsAll();

			

			for ( CurrencyPair cp : tradable_pairs)
			{
				Ticker ticker=null;
				try {
					ticker = marketDataService.getTicker(cp);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		
				System.out.println(ticker.getCurrencyPair().toString()+" "+ticker.getLast());	
			}
				
		}
		
		
//		MarketDataService marketDataService = bitstamp.getMarketDataService();
//
//		Ticker ticker=null;
//		try {
//			ticker = marketDataService.getTicker(CurrencyPair.BTC_USD);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		System.out.println(ticker.toString());		
		
	}

}
