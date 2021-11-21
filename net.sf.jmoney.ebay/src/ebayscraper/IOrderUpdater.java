package ebayscraper;

import java.util.Set;

public interface IOrderUpdater {

	IItemUpdater createNewItemUpdater(long netCost);

	Set<? extends IItemUpdater> getItemUpdaters();

	long getPostageAndPackaging();

	void setPostageAndPackaging(long postageAndPackagingAmount);


}
