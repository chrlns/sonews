/*
 *   SONEWS News Server
 *   see AUTHORS for the list of contributors
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

package org.sonews.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonews.util.Pair;

/**
 * An aggregated group is a group consisting of several "real" group.
 * @author Christian Lins
 * @since sonews/1.0
 */
class AggregatedGroup extends Channel
{

  static class GroupElement
  {
    private Group group;
    private long  offsetStart, offsetEnd;
    
    public GroupElement(Group group, long offsetStart, long offsetEnd)
    {
      this.group       = group;
      this.offsetEnd   = offsetEnd;
      this.offsetStart = offsetStart;
    }
  }

  public static List<Channel> getAll()
  {
    List<Channel> all = new ArrayList<Channel>();
    all.add(getByName("agg.test"));
    return all;
  }

  public static AggregatedGroup getByName(String name)
  {
    if("agg.test".equals(name))
    {
      AggregatedGroup agroup = new AggregatedGroup(name);
      agroup.addGroup(Group.getByName("agg.test0"), 0, 1000);
      agroup.addGroup(Group.getByName("agg.test1"), 2000, 4000);
      return agroup;
    }
    else
      return null;
  }

  private GroupElement[] groups = new GroupElement[2];
  private String         name;

  public AggregatedGroup(String name)
  {
    this.name = name;
  }

  private long aggIdxToIdx(long aggIdx)
    throws StorageBackendException
  {
    assert groups != null && groups.length == 2;
    assert groups[0] != null;
    assert groups[1] != null;

    // Search in indices of group one
    List<Long> idxs0 = groups[0].group.getArticleNumbers();
    Collections.sort(idxs0);
    for(long idx : idxs0)
    {
      if(idx == aggIdx)
      {
        return idx;
      }
    }

    // Given aggIdx must be an index of group two
    List<Long> idxs1 = groups[1].group.getArticleNumbers();
    return 0;
  }
  
  private long idxToAggIdx(long idx)
  {
    return 0;
  }

  /**
   * Adds the given group to this aggregated set.
   * @param group
   * @param offsetStart Lower limit for the article ids range
   */
  public void addGroup(Group group, long offsetStart, long offsetEnd)
  {
    this.groups[groups[0] == null ? 0 : 1]
      = new GroupElement(group, offsetStart, offsetEnd);
  }

  @Override
  public Article getArticle(long idx)
    throws StorageBackendException
  {
    Article article = null;

    for(GroupElement groupEl : groups)
    {
      if(groupEl.offsetStart <= idx && groupEl.offsetEnd >= idx)
      {
        article = groupEl.group.getArticle(idx - groupEl.offsetStart);
        break;
      }
    }

    return article;
  }

  @Override
  public List<Pair<Long, ArticleHead>> getArticleHeads(
    final long first, final long last)
    throws StorageBackendException
  {
    List<Pair<Long, ArticleHead>> heads = new ArrayList<Pair<Long, ArticleHead>>();
    
    for(GroupElement groupEl : groups)
    {
      List<Pair<Long, ArticleHead>> partHeads = new ArrayList<Pair<Long, ArticleHead>>();
      if(groupEl.offsetStart <= first && groupEl.offsetEnd >= first)
      {
        long end = Math.min(groupEl.offsetEnd, last);
        partHeads = groupEl.group.getArticleHeads
          (first - groupEl.offsetStart, end - groupEl.offsetStart);
      }
      else if(groupEl.offsetStart <= last && groupEl.offsetEnd >= last)
      {
        long start = Math.max(groupEl.offsetStart, first);
        partHeads = groupEl.group.getArticleHeads
          (start - groupEl.offsetStart, last - groupEl.offsetStart);
      }

      for(Pair<Long, ArticleHead> partHead : partHeads)
      {
        heads.add(new Pair<Long, ArticleHead>(
          partHead.getA() + groupEl.offsetStart, partHead.getB()));
      }
    }

    return heads;
  }

  @Override
  public List<Long> getArticleNumbers()
    throws StorageBackendException
  {
    List<Long> articleNumbers = new ArrayList<Long>();
    
    for(GroupElement groupEl : groups)
    {
      List<Long> partNums = groupEl.group.getArticleNumbers();
      for(Long partNum : partNums)
      {
        articleNumbers.add(partNum + groupEl.offsetStart);
      }
    }

    return articleNumbers;
  }

  @Override
  public long getIndexOf(Article art)
    throws StorageBackendException
  {
    for(GroupElement groupEl : groups)
    {
      long idx = groupEl.group.getIndexOf(art);
      if(idx > 0)
      {
        return idx;
      }
    }
    return -1;
  }

  public long getInternalID()
  {
    return -1;
  }

  @Override
  public String getName()
  {
    return this.name;
  }

  @Override
  public long getFirstArticleNumber()
    throws StorageBackendException
  {
    long first = Long.MAX_VALUE;

    for(GroupElement groupEl : groups)
    {
      first = Math.min(first, groupEl.group.getFirstArticleNumber() + groupEl.offsetStart);
    }

    return first;
  }

  @Override
  public long getLastArticleNumber()
    throws StorageBackendException
  {
    long last = 1;

    for(GroupElement groupEl : groups)
    {
      last = Math.max(last, groupEl.group.getLastArticleNumber() + groupEl.offsetStart);
    }

    return last + getPostingsCount(); // This is a hack
  }
  
  public long getPostingsCount()
    throws StorageBackendException
  {
    long postings = 0;

    for(GroupElement groupEl : groups)
    {
      postings += groupEl.group.getPostingsCount();
    }

    return postings;
  }

  public boolean isDeleted()
  {
    return false;
  }

  public boolean isWriteable()
  {
    return false;
  }

}
