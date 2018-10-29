package arbitrage;

import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.marketdata.params.Params;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;


import org.knowm.xchange.Exchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.poloniex.*;
import org.knowm.xchange.poloniex.dto.PoloniexException;
import org.knowm.xchange.poloniex.dto.marketdata.PoloniexMarketData;
import org.knowm.xchange.poloniex.dto.marketdata.PoloniexTicker;
import org.knowm.xchange.poloniex.service.*;

public class PoloniexMarketDataServiceV2 extends PoloniexMarketDataService implements MarketDataService {
	private final long cache_delay = 1000L;	
	private HashMap<String, PoloniexMarketData> TickermarketData;
	private long next_refresh = System.currentTimeMillis() + cache_delay;	
	
	public PoloniexMarketDataServiceV2(Exchange exchange) {
		super(exchange);
		// TODO Auto-generated constructor stub
	}

	public List<PoloniexTicker> getPoloniexTickers (List<CurrencyPair> currencyPairs) throws ExchangeException
	{
		    String command = "returnTicker";
		    long now = System.currentTimeMillis();		    
		    List<String> pairStrings = new Vector<String>();
		    List<PoloniexTicker> tickers =  new Vector<PoloniexTicker>();
		    for ( CurrencyPair cp : currencyPairs)
		    {
		    	pairStrings.add(PoloniexUtils.toPairString(cp));
		    }
			  try {
				  		    	  
			    TickermarketData = poloniex.getTicker(command);
			  } catch (PoloniexException e) {
			    throw PoloniexErrorAdapter.adapt(e);
			  } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
			    // also nice to take a short break on an error
			    next_refresh = now + cache_delay;
			  }
		    

			for ( String s : pairStrings)
			{
				PoloniexMarketData data = TickermarketData.get(s);
			    if (data == null) {
			    		break;
				}
			    tickers.add(new PoloniexTicker(data,PoloniexUtils.toCurrencyPair(s)));
			}
			
		    return tickers;
	}
	
	  @Override
	  public List<Ticker> getTickers(Params params) throws IOException {
		  TickersParams tparams = (TickersParams)params;
		  List<Ticker> tickers = new Vector<Ticker>();
		    try {
		        List<PoloniexTicker> poloniexTickers = getPoloniexTickers((List<CurrencyPair>)tparams.getCurrencyPairs());
		        if (poloniexTickers == null) {
		        	return null;
		        }
		        for ( PoloniexTicker pticker : poloniexTickers)
		        {
		        	tickers.add(PoloniexAdapters.adaptPoloniexTicker(pticker, pticker.getCurrencyPair()));
		        }
		        
		      } catch (PoloniexException e) {
		        throw PoloniexErrorAdapter.adapt(e);
		      }		  
		  
	    return tickers;	    
	  }
	
}
