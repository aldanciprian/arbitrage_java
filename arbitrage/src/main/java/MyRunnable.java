package main.java;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.marketdata.params.Params;

public class MyRunnable implements Runnable {

	public String exchange_name = null;

	public MyRunnable(Object parameter) {
		// store parameter for later user
		exchange_name = (String) parameter;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		MarketDataService marketDataService = App.exchanges.get(exchange_name).getMarketDataService();
		Params pairs = new TickersParams(App.pairPerExchange.get(exchange_name));
		Map<CurrencyPair, Ticker> exchange_tickers = new HashMap<CurrencyPair, Ticker>();

		boolean hasMethod = true;
		try {
			System.out.println("Preparing to execute getTickers " + marketDataService.getClass().toString());			
//			marketDataService.getClass().getMethod("getTickers", (Class<Params>) null);
			marketDataService.getClass().getMethod("getTickers");
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			hasMethod = false;
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			hasMethod = false;
		}

		if (hasMethod) {
			try {

				List<Ticker> tickers = marketDataService.getTickers(pairs);
				for (Ticker tick : tickers) {
//					System.out.println(tick);
					exchange_tickers.put(tick.getCurrencyPair(), tick);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		} else {
			for (CurrencyPair cp : App.pairPerExchange.get(exchange_name)) {
				Ticker ticker = null;
				try {
					ticker = marketDataService.getTicker(cp);
				} catch (IOException eio) {
					// TODO Auto-generated catch block
					eio.printStackTrace();
				}
				if (ticker != null) {
					System.out.println(exchange_name + "   " + ticker);
					exchange_tickers.put(ticker.getCurrencyPair(), ticker);
				}
				try {
					Thread.sleep(300);
				} catch (InterruptedException ex) {
					// TODO Auto-generated catch block
					ex.printStackTrace();
				}
			}

		}

		App.all_tickers.put(exchange_name, exchange_tickers);

	}

}
