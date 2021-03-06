/*
 * This file is part of Search.
 *
 *  Search is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  earch is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Search.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2015
 */
package search.format;
import java.util.BitSet;
import edu.luc.nmerge.mvd.Pair;
import java.util.ArrayList;

/**
 * Represent the full position of a term in an MVD. We have one start 
 * position in the MVD and potentially multiple end-points
 * @author desmond
 */
public class Position 
{
    BitSet versions;
    int start;
    int[] ends;
    /**
     * Create a position and compute the versions and end-points
     * @param pairs an array of pairs from an MVD
     * @param index the start index of the first pair to look at
     * @param offset the offset into that pair where our text begins
     * @param start the mvd start-offset (global, not version-based)
     */
    Position( ArrayList<Pair> pairs, int index, int offset, int start )
    {
        Pair p = pairs.get(index);
        BitSet bs = p.versions;
        this.start = start;
        this.versions = new BitSet();
        ArrayList<Integer> list = new ArrayList<Integer>();
        for ( int v=bs.nextSetBit(0);v!=-1;v=bs.nextSetBit(v+1) )
        {
            int end = start;
            boolean finished = false;
            int pIndex = index;
            int pOffset = offset;
            p = pairs.get(pIndex);
            while ( !finished )
            {
                char[] data = p.getChars();
                for ( int i=pOffset;i<data.length;i++ )
                {
                    // we follow each version from the start
                    // until we encounter a non-letter
                    if ( !Character.isLetter(data[i]) )
                    {
                        int j;
                        for ( j=0;j<list.size();j++ )
                            if ( list.get(j) == end )
                                break;
                        if ( j==list.size() )
                            list.add( end );
                        this.versions.set(v);
                        finished = true;
                    }
                    else
                        end++;
                }
                // do we need to load another pair?
                while ( !finished && pIndex < pairs.size()-1 )
                {
                    p = pairs.get(++pIndex);
                    if ( p.versions.nextSetBit(v)==v && p.length()>0 )
                    {
                        pOffset = 0;
                        break;
                    }
                }
            }
        }
        // convert to a simple array
        this.ends = new int[list.size()];
        for ( int i=0;i<list.size();i++ )
            this.ends[i] = list.get(i);
    }
    /**
     * Get the versions that share this term
     * @return a bitset
     */
    BitSet getVersions()
    {
        return this.versions;
    }
    /**
     * Do we overlap some other position?
     * @param other the other position
     * @return true if there is some overlap
     */
    boolean overlaps( Position other )
    {
        if ( this.start <= other.start )
        {
            for ( int i=0;i<this.ends.length;i++ )
                if ( this.ends[i] > other.start )
                    return true;
        }
        else
        {
            for ( int i=0;i<other.ends.length;i++ )
                if ( other.ends[i] > this.start )
                    return true;
        }
        return false;
    }
}
