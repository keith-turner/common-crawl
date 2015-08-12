
Observer Design Doc
===================

Sample Data

    a.com/page1 links to c.com, b.com
    b.com links to c.com/page1, c.com
    d.com links to c.com

Resulting Fluo Table

    row               cf        cq              value
    --------------------------------------------------
    d:com.a           rank      1:com.a/page1   1
    d:com.b           rank      2:com.b         2
    d:com.c           rank      3:com.c         3
                                1:com.c/page1   1
    d:com.d           rank      1:com.d         1
    p:com.a/page1     page      cur             {"outlinkcount": 2, "outlinks":[c.com, b.com]}
                      stats     pagescore       1
    p:com.b           inlinks   com.a/pag1      anchorText
                      page      cur             {"outlinkcount": 2, "outlinks":[c.com/page1, c.com]}
                      stats     pagescore       2
                      stats     inlinkscount    1
    p:com.c           inlinks   com.a/page1     anchorText
                                com.b           anchorText
                                com.d           anchorText
                      stats     inlinkscount    3
                      stats     pagescore       3
    p:com.c/page1     inlinks   com.b           anchorText
                      stats     pagescore       1
                      stats     inlinkscount    1
    p:com.d           page      cur             {"outlinkcount": 1, "outlinks":[c.com]}
                      stats     pagescore       1

Below are available operations:

    get(row, col) -> value
    set(row, col, value)
    del(row, col)

PageLoader is called with `pageUri` & `pageJson`

    curJson = get(pageUri, page:cur)
    if curJson != pageJson
      set(pageUri, page:new, pageJson)

PageObserver watches `page:new` is called with `pageUri`

    curJson = get(pageUri, page:cur)
    newJson = get(pageUri, page:new)

    newLinks,delLinks = compare(curJson, newJson)

    for link in newLinks:
      set(linkUri, update:inlinks, Update[add,pageUri,anchorText])

    for link in delLinks
      set(linkUri, update:inlinks, Update[del,pageUri,anchorText])

    if curJson == null:
      pageScore = get(pageUri, stats:pagescore).toInteger(0)

      if pageScore != 0:
        del(pageDomain, rank:pageScore:pageUri)
      
      set(pageUri, stats:pagescore, pageScore+1)
      set(pageDomain, rank:pageScore+1:pageUri)

    set(pageUri, page:cur, newJson)
    del(pageUri, page:new)

InlinksObserver weakly watches `update:inlinks` is called with `pageUri`

    List<Update> updates = get(pageUri, update:inlinks)

    change = 0

    for update in updates:
      if update.action == del:
        change--
        del(pageUri, inlinks:update.linkUri)
      elif update.action == add:
        change++
        set(pageUri, inlinks:update.linkUri, update.anchorText)

    if change != 0:
      curCount = get(pageUri, stats:inlinkcount)
      curScore = get(pageUri, stats:pagescore).toInteger(0)

      if pageScore != 0:
        del(pageDomain, rank:curScore:pageUri)

      set(pageDomain, rank:curScore+change:pageUri)
      set(pageUri, stats:inlinkcount, curCount+change)
      set(pageUri, stats:pagescore, curScore+change)
