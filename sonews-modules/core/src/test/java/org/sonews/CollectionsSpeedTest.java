/*
 *   SONEWS News Server
 *   Copyright (C) 2009-2015  Christian Lins <christian@lins.me>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sonews;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * Tests the speed of LinkedList and ArrayList.
 * @author Christian Lins
 * @since sonews/0.5.0
 */
public class CollectionsSpeedTest 
{

  public static void main(String[] args)
  {
    List arrayList  = new ArrayList();
    List linkedList = new LinkedList();
    
    int numElements = 100000;
    
    System.out.println("ArrayList.add(): " + add(arrayList, numElements) + "ms");
    System.out.println("LinkenList.add(): " + add(linkedList, numElements) + "ms");
    System.out.println("ArrayList.iterate: " + iterate(arrayList) + "ms");
    System.out.println("LinkedList.iterate: " + iterate(linkedList) + "ms");
  }
  
  private static long add(List list, int numElements)
  {
    long start = System.currentTimeMillis();
    
    for(int n = 0; n < numElements; n++)
    {
      list.add(new Object());
    }
    
    return System.currentTimeMillis() - start;
  }
  
  private static long iterate(List list)
  {
    long start = System.currentTimeMillis();
    
    ListIterator iter = list.listIterator();
    while(iter.hasNext())
    {
      iter.next();
    }
    
    return System.currentTimeMillis() - start;
  }
  
}
