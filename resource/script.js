window.picstory = new Object();
window.picstory.sizes = [ 800, 600, 400, 300, 200 ];

function donePage()
{
	if(document.body.className.indexOf("story") != -1)
	{
		initStory();
	}
}

function initStory()
{
	// Get list of all pics and extract data
	var picList = document.getElementsByTagName('img');
	var tempPics = [];
	for(var i=0; i<picList.length; i++)
	{
		var hiddenPic = picList[i];
		var noscript = hiddenPic.parentNode;
		if(noscript.nodeName.toLowerCase() != 'noscript')
		{
			continue;
		}
		tempPics.push(hiddenPic);
	}

	window.picstory.pics = [];
	for(var i=0; i<tempPics.length; i++)
	{
		var hiddenPic = tempPics[i];
		var noscript = hiddenPic.parentNode;
		noscript.removeChild(hiddenPic);
		
		// Create new img tag
		var pic = document.createElement('img');
		pic.alt = ''; // No alt because there's a caption below
		pic.picstory = new Object();
		pic.picstory.src800 = hiddenPic.src;
		noscript.parentNode.insertBefore(pic, noscript);

		// Put data into img tag		
		pic.picstory.div = noscript.parentNode;
		
		var className = pic.picstory.div.className;
		if(className.indexOf('portrait') != -1)
		{
			pic.picstory.portrait = true;
		}
		else
		{
			pic.picstory.portrait = false;
		}
		
		var re = / size([0-9]+)x([0-9]+)/;
		var match = re.exec(className);
		pic.picstory.width = Number(match[1]);
		pic.picstory.height = Number(match[2]);
		
		pic.picstory.current = 0;
		
		window.picstory.pics.push(pic);
	}
	
	// Now process as if page size changed
	sizeChanged();
	
	// And listen out for future size changes
	setInterval(function()
	{
		var newSize = getWindowSize();
		if(newSize != window.picstory.lastSize)
		{
			sizeChanged();
		}
	}, 100);
}

function getWindowSize()
{
	var result = new Object();
	if(window.innerWidth)
 	{
		result.width = window.innerWidth;
		result.height = window.innerHeight;
	}
	else
	{
		result.width = 800;
		result.height = 600;
	}
	return result;
}

function calculateSize(width, height, horizontal, vertical)
{
	var reducedWidth1 = width, reducedHeight1 = height;
	if(width > horizontal)
	{
		reducedHeight1 = Math.round(height * horizontal / width);
		reducedWidth1 = horizontal;
	}
	var reducedWidth2 = width, reducedHeight2 = height;
	if(height > vertical)
	{
		reducedWidth2 = Math.round(width * vertical / height);
		reducedHeight2 = vertical;
	}
	var result = new Object();
	result.width = Math.min(reducedWidth1, reducedWidth2);
	result.height = Math.min(reducedHeight1, reducedHeight2);
	return result;
}

function sizeChanged()
{
	// Get page width
	var windowSize = getWindowSize();
	window.picstory.lastSize = windowSize;
	var width = windowSize.width, height = windowSize.height;
	
	// Account for padding
	var padding = 10;
	width -= 2 * padding;
	height -= 2 * padding;
	
	// Multiply by device pixel ratio if there is one so that we use
	// a higher-res image than the CSS pixel count
	if(!window.devicePixelRatio)
	{
		window.devicePixelRatio = 1;
	}

	// Process all pictures
	for(var i=0; i<window.picstory.pics.length; i++)
	{
		var pic = window.picstory.pics[i];
		var data = pic.picstory;
		
		// Work out size we will be drawing this image
		var desiredPixels = calculateSize(data.width, data.height,
			width * window.devicePixelRatio,
			height * window.devicePixelRatio);
		
		// Select appropriate size
		var desiredSize = 0;
		for(var size = 0; size < window.picstory.sizes.length - 1; size++)
		{
			// Calculate size of *next* smaller size
			var sizePixels = calculateSize(data.width, data.height,
				window.picstory.sizes[size+1], (window.picstory.sizes[size+1] * 3) / 4);

			// If our desired size is bigger, use that
			if(desiredPixels.width > sizePixels.width
				|| desiredPixels.height > sizePixels.height)
			{
				desiredSize = window.picstory.sizes[size];
				break;
			}			
		}
		// If we didn't find a small enough size yet, use the smallest
		if(desiredSize == 0)
		{
			desiredSize = window.picstory.sizes[window.picstory.sizes.length - 1];
		}
		
		// Load new size, unless it's smaller than current in which case there's
		// no point loading another one
		if(data.current < desiredSize)
		{
			pic.src = data.src800.replace(/w800\.jpg$/, 'w' + desiredSize + '.jpg');
			data.current = desiredSize;
		}
		
		// Update picture max height (don't do max width, that is fixed to 100%
		// and actually works when you resize browser)
		pic.style.maxHeight = height + 'px';
	}
}