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
import org.knowm.xchange.bitfinex.v2.BitfinexExchange;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.FundingRecord;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.poloniex.PoloniexExchange;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.params.TradeHistoryParamsAll;



public class App {
	
	public static List<String> exchangesNames = null;
	public static Map<String,List<CurrencyPair>> tradable_pairs = null;
	public static Map<String,Exchange> exchanges = null;
	public static Map<String,List<CurrencyPair>> pairPerExchange = null;
	public static Map<String,Map<CurrencyPair,Ticker>> all_tickers = null;
	
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

		
		tradable_pairs = new HashMap<String,List<CurrencyPair>>();

		
		exchangesNames  =  new Vector<String>();
		exchangesNames.add("bitfinex");
		exchangesNames.add("binance");
//		exchangesNames.add("poloniex");
		
		exchanges = new HashMap<String,Exchange>();
		
		connectExchanges();
		
		all_tickers = new  HashMap<String,Map<CurrencyPair,Ticker>>();
		
		Map<String,List<CurrencyPair>> all_eth_symbols = new HashMap<String,List<CurrencyPair>>();
		
		for ( String key: exchangesNames )
		{
			List<CurrencyPair> eth_symbols = new Vector<CurrencyPair>();
			List<CurrencyPair> symbols = exchanges.get(key).getExchangeSymbols();
			
			for (CurrencyPair cp : symbols)
			{
				if ( cp.counter == Currency.ETH )
				{
					eth_symbols.add(cp);
				}
			}
			all_eth_symbols.put(key, eth_symbols);
		}
		
		
		System.out.println();
		
		for ( String key: exchangesNames ) 
		{
			for ( String key2: exchangesNames )
			{
				if ( key != key2 )
				{
					List<CurrencyPair> common_symbols = new Vector<CurrencyPair>();
					for ( CurrencyPair elem: all_eth_symbols.get(key) )
					{
						for ( CurrencyPair elem2: all_eth_symbols.get(key2) )
						{
							if ( elem.compareTo(elem2) == 0 )
							{
								common_symbols.add(elem);
								break;
							}
						}
					}
					if ( !tradable_pairs.containsKey(key+":"+key2) && !tradable_pairs.containsKey(key2+":"+key) )
					{
						tradable_pairs.put(key+":"+key2,common_symbols);
					}
				}
			}

		}

		pairPerExchange = new HashMap<String,List<CurrencyPair>>();
		for (String key : tradable_pairs.keySet())
		{
			String[] exchanges_keys = key.split(":");
			for ( CurrencyPair cp : tradable_pairs.get(key))
			{
				if (pairPerExchange.containsKey(exchanges_keys[0])) {
					List<CurrencyPair> temp_list_cp = new Vector<CurrencyPair>();
					for (CurrencyPair cp2 : pairPerExchange.get(exchanges_keys[0])) {
						temp_list_cp.add(cp2);
					}
					
					boolean contains = false;
					for (CurrencyPair cp2 : temp_list_cp) {
						if (cp.compareTo(cp2) == 0) {
							contains = true;
							break;
						}
					}
					if ( contains == false )
					{
						pairPerExchange.get(exchanges_keys[0]).add(cp);						
					}
				} else {
					System.out.println(exchanges_keys[0]+"  adding to list");
					List<CurrencyPair> list_cp = new Vector<CurrencyPair>();
					list_cp.add(cp);
					pairPerExchange.put(exchanges_keys[0], list_cp);
				}
			}

			
			for ( CurrencyPair cp : tradable_pairs.get(key))
			{
				if (pairPerExchange.containsKey(exchanges_keys[1])) {
					List<CurrencyPair> temp_list_cp = new Vector<CurrencyPair>();
					for (CurrencyPair cp2 : pairPerExchange.get(exchanges_keys[1])) {
						temp_list_cp.add(cp2);
					}
					
					boolean contains = false;
					for (CurrencyPair cp2 : temp_list_cp) {
						if (cp.compareTo(cp2) == 1) {
							contains = true;
							break;
						}
					}
					if ( contains == false )
					{
						pairPerExchange.get(exchanges_keys[1]).add(cp);						
					}
				} else {
					List<CurrencyPair> list_cp = new Vector<CurrencyPair>();
					list_cp.add(cp);
					pairPerExchange.put(exchanges_keys[1], list_cp);
				}
			}			
			
		}

		all_tickers.clear();
		List<Thread> workers = new Vector<Thread>();
		for ( String key: exchangesNames )
		{
			MyRunnable r = new MyRunnable(key);
			Thread th = new Thread(r); 
			workers.add(th);
			
			th.start();
		}
		
		for ( Thread th : workers)
		{
			try {
				th.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		for (String key : tradable_pairs.keySet())
		{
			String[] exchanges_keys = key.split(":");
			for ( CurrencyPair cp : tradable_pairs.get(key))
			{
				System.out.println(key.toString());
				System.out.print(cp.toString()+"  ");
				try
				{
					if (all_tickers.get(exchanges_keys[0]).containsKey(cp)) 
					{
						System.out.print(exchanges_keys[0]+": ");					
						Ticker tick = all_tickers.get(exchanges_keys[0]).get(cp);
						System.out.print("SELL "+tick.getAsk().toString()+"  BUY "+tick.getBid().toString());
						System.out.print("  -  ");
					}
					if (all_tickers.get(exchanges_keys[1]).containsKey(cp)) 
					{
						System.out.print(exchanges_keys[1]+": ");					
						Ticker tick = all_tickers.get(exchanges_keys[1]).get(cp);
						System.out.print("SELL "+tick.getAsk().toString()+"  BUY "+tick.getBid().toString());
					}				
				} catch (NullPointerException e)
				{
					System.out.println(e.getMessage());
				}
				System.out.println();
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
