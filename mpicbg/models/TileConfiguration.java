/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 *
 */
package mpicbg.models;

import java.util.ArrayList;
import java.util.Collection;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;


/**
 * A configuration of rigidly transformed tiles.
 * 
 * Add all tiles that build a common interconnectivity graph to one
 * configuration, fix at least one of the tiles and optimize the configuration.
 * 
 * @version 0.2b
 *
 */
public class TileConfiguration
{
	final static private DecimalFormat decimalFormat = new DecimalFormat();
	final static private DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();

	final private ArrayList< Tile > tiles = new ArrayList< Tile >();
	final public ArrayList< Tile > getTiles(){ return tiles; }
	
	final private ArrayList< Tile > fixedTiles = new ArrayList< Tile >();
	final public ArrayList< Tile > getFixedTiles(){ return fixedTiles; }
	
	private double minError = Double.MAX_VALUE;
	private double maxError = 0.0;
	private double error = Double.MAX_VALUE;
	
	public TileConfiguration()
	{
		decimalFormatSymbols.setGroupingSeparator( ',' );
		decimalFormatSymbols.setDecimalSeparator( '.' );
		decimalFormat.setDecimalFormatSymbols( decimalFormatSymbols );
		decimalFormat.setMaximumFractionDigits( 3 );
		decimalFormat.setMinimumFractionDigits( 3 );		
	}
	
	/**
	 * Add a single tile.
	 * 
	 * @param t
	 */
	final public void addTile( Tile t ){ tiles.add( t ); }
	
	/**
	 * Add a collection of tiles.
	 * 
	 * @param t
	 */
	final public void addTiles( Collection< Tile > t ){ tiles.addAll( t ); }
	
	/**
	 * Fix a single tile.
	 * 
	 * @param t
	 */
	final public void fixTile( Tile t ){ fixedTiles.add( t ); }
	
	/**
	 * Update all correspondences in all tiles and estimate the average
	 * displacement. 
	 */
	final private void update()
	{
		double cd = 0.0;
		minError = Double.MAX_VALUE;
		maxError = 0.0;
		for ( Tile t : tiles )
		{
			t.update();
			double d = t.getDistance();
			if ( d < minError ) minError = d;
			if ( d > maxError ) maxError = d;
			cd += d;
		}
		cd /= tiles.size();
		error = cd;
	}
	
	/**
	 * Minimize the displacement of all correspondence pairs of all tiles.
	 * 
	 * @param maxError do not accept convergence if error is > max_error
	 * @param maxIterations stop after that many iterations even if there was
	 *   no minimum found
	 * @param maxPlateauwidth convergence is reached if the average slope in
	 *   an interval of this size is 0.0 (in double accuracy).  This prevents
	 *   the algorithm from stopping at plateaus smaller than this value.
	 */
	public void optimize(
			float maxError,
			int maxIterations,
			int maxPlateauwidth ) throws NotEnoughDataPointsException 
	{
		ErrorStatistic observer = new ErrorStatistic();
		
		int i = 0;
		
		while ( i < maxIterations )  // do not run forever
		{
			for ( Tile tile : tiles )
			{
				if ( fixedTiles.contains( tile ) ) continue;
				tile.update();
				tile.fitModel();
				tile.update();
			}
			update();
			observer.add( error );			
			
			if (
					i >= maxPlateauwidth &&
					error < maxError &&
					Math.abs( observer.getWideSlope( maxPlateauwidth ) ) <= 0.0001 &&
					Math.abs( observer.getWideSlope( maxPlateauwidth / 2 ) ) <= 0.0001 )
			{
				break;
			}
			++i;
		}
		
		System.out.println( "Successfully optimized configuration of " + tiles.size() + " tiles after " + i + " iterations:" );
		System.out.println( "  average displacement: " + decimalFormat.format( error ) + "px" );
		System.out.println( "  minimal displacement: " + decimalFormat.format( minError ) + "px" );
		System.out.println( "  maximal displacement: " + decimalFormat.format( maxError ) + "px" );
	}

}